"""
CopilotLens MCP Server
======================
Exposes codebase intelligence as MCP tools for GitHub Copilot (Agent Mode).

Usage:
    python server.py --repo /path/to/your/project

MCP Tools exposed:
    - get_file_health      : Score a specific file (0-100)
    - get_hotspots         : Top N high-churn / risky files
    - get_codebase_summary : Overall health overview
    - get_dead_code        : Unused functions/classes
    - get_dependency_graph : Import dependency map
    - get_file_dependencies: Dependencies for a specific file
    - get_module_owners    : Who owns each module (git blame aggregated)
    - generate_copilot_instructions : Generate copilot-instructions.md
    - get_dashboard_url    : URL to the visual dashboard

Dashboard:
    Served at http://localhost:8765
"""

import argparse
import json
import os
import sys
import threading
from http.server import HTTPServer, BaseHTTPRequestHandler
from pathlib import Path

# Add parent dir to path so we can import analyzers
sys.path.insert(0, str(Path(__file__).parent))

from mcp.server.fastmcp import FastMCP
from analyzers.git_analyzer import GitAnalyzer
from analyzers.code_health import CodeHealthScorer
from analyzers.dependency import DependencyAnalyzer
from analyzers.dead_code import DeadCodeDetector
from analyzers.cache import AnalysisCache
from analyzers.why_analyzer import WhyAnalyzer
from analyzers.search_analyzer import CodebaseSearch
from analyzers.blast_radius import BlastRadiusAnalyzer

# ─── Argument Parsing ──────────────────────────────────────────────────────────

def get_repo_path() -> str:
    """Get repo path from CLI args or environment variable."""
    path = None
    if "--repo" in sys.argv:
        idx = sys.argv.index("--repo")
        if idx + 1 < len(sys.argv):
            path = sys.argv[idx + 1]
    
    # If the IDE failed to expand the VS Code-style variable, or it's missing, use a fallback
    if not path or path == "${workspaceFolder}":
        path = os.environ.get("COPILOTLENS_REPO")
        
    if not path or path == "${workspaceFolder}":
        # Fallback to the repository root (parent of the mcp_server directory)
        path = str(Path(__file__).parent.parent.absolute())
        
    return path


REPO_PATH = get_repo_path()
DASHBOARD_PORT = int(os.environ.get("COPILOTLENS_PORT", "8765"))

# ─── Initialize Analyzers ──────────────────────────────────────────────────────

cache = AnalysisCache(REPO_PATH)
git_analyzer = GitAnalyzer(REPO_PATH)
health_scorer = CodeHealthScorer(REPO_PATH)
dep_analyzer = DependencyAnalyzer(REPO_PATH)
dead_code_detector = DeadCodeDetector(REPO_PATH)
why_analyzer = WhyAnalyzer(REPO_PATH)
search_engine = CodebaseSearch(REPO_PATH)
blast_analyzer = BlastRadiusAnalyzer(REPO_PATH, dep_analyzer, git_analyzer)

# ─── MCP Server ────────────────────────────────────────────────────────────────

mcp = FastMCP(
    name="CopilotLens",
    instructions=f"""
You have access to CopilotLens — a codebase intelligence layer for the repository at: {REPO_PATH}

Use these tools to:
- Understand code health BEFORE suggesting refactors
- Identify risky/hotspot files that need extra care
- Check who owns a module before suggesting changes
- Find dead code that can be safely removed
- Understand architectural dependencies before restructuring

Always call get_file_health() before suggesting changes to a specific file.
Always call get_hotspots() when asked about risky or problematic areas of the codebase.
"""
)


@mcp.tool()
def get_file_health(file_path: str) -> str:
    """
    Get the health score and breakdown for a specific file.
    Returns a score from 0 (critical) to 100 (excellent) with detailed markers explaining the score.
    Use this BEFORE suggesting refactoring or changes to any file.
    
    Args:
        file_path: Relative path to the file from the repository root (e.g., 'src/main/App.java')
    """
    cached = cache.get(f"health:{file_path}")
    if cached:
        return json.dumps(cached, indent=2)
    
    result = health_scorer.score_file(file_path)
    result["path"] = file_path
    result["repo"] = REPO_PATH
    
    # Add actionable advice based on markers
    advice = []
    for marker in result.get("markers", []):
        if marker["impact"] < 0:
            mid = marker["id"]
            if mid == "file_too_large":
                advice.append("⚠️ This file is too large. Consider splitting it into smaller modules.")
            elif mid == "long_functions":
                advice.append("⚠️ Long functions detected. Extract into smaller, focused functions.")
            elif mid == "deeply_nested":
                advice.append("⚠️ Deep nesting found. Consider early returns or extracting methods.")
            elif mid == "too_many_todos":
                advice.append(f"⚠️ {marker.get('detail', 'Multiple TODO/FIXME')} — technical debt accumulating.")
            elif mid == "no_comments":
                advice.append("⚠️ No documentation. Add docstrings/comments before modifying.")
            elif mid == "high_complexity":
                advice.append("⚠️ High cyclomatic complexity. Any change here has high defect risk.")
    
    result["copilot_advice"] = advice
    cache.set(f"health:{file_path}", result)
    return json.dumps(result, indent=2)


@mcp.tool()
def get_hotspots(top_n: int = 10) -> str:
    """
    Get the top N hotspot files — files with the highest churn rate (frequently changed).
    High-churn files are statistically the most defect-prone.
    Use this to identify risky areas before any large refactoring effort.
    
    Args:
        top_n: Number of hotspot files to return (default: 10, max: 30)
    """
    top_n = min(top_n, 30)
    cached = cache.get(f"hotspots:{top_n}")
    if cached:
        return json.dumps(cached, indent=2)
    
    hotspots = git_analyzer.get_hotspots(top_n)
    
    result = {
        "hotspots": hotspots,
        "insight": (
            f"The top {len(hotspots)} hotspot files account for the highest change frequency. "
            "Files with CRITICAL or HIGH risk need careful review before modification. "
            "Consider adding tests before touching these files."
        ),
        "repo": REPO_PATH
    }
    
    cache.set(f"hotspots:{top_n}", result)
    return json.dumps(result, indent=2)


@mcp.tool()
def get_codebase_summary() -> str:
    """
    Get a comprehensive health summary of the entire codebase.
    Returns average health score, grade distribution, worst and best files,
    git activity summary, and high-level architectural insights.
    Use this for an overall assessment before starting any large task.
    """
    cached = cache.get("summary")
    if cached:
        return json.dumps(cached, indent=2)
    
    health_summary = health_scorer.get_summary()
    repo_summary = git_analyzer.get_repo_summary()
    
    result = {
        "repo_path": REPO_PATH,
        "health": health_summary,
        "git_activity": repo_summary,
        "dashboard_url": f"http://localhost:{DASHBOARD_PORT}",
        "interpretation": _interpret_summary(health_summary)
    }
    
    cache.set("summary", result)
    return json.dumps(result, indent=2)


@mcp.tool()
def get_dead_code() -> str:
    """
    Find functions, classes, and variables that appear to be defined but never used.
    These are safe candidates for removal, which reduces complexity and maintenance burden.
    Returns symbol name, file location, type (function/class), and confidence level.
    """
    cached = cache.get("dead_code")
    if cached:
        return json.dumps(cached, indent=2)
    
    dead = dead_code_detector.find_dead_code()
    
    result = {
        "dead_code_candidates": dead,
        "count": len(dead),
        "insight": (
            f"Found {len(dead)} potentially unused symbols. "
            "HIGH confidence items are very likely safe to remove. "
            "MEDIUM confidence items may be used via reflection, dynamic dispatch, or external callers — verify before removing."
        ),
        "repo": REPO_PATH
    }
    
    cache.set("dead_code", result)
    return json.dumps(result, indent=2)


@mcp.tool()
def get_dependency_graph() -> str:
    """
    Get the full import/dependency graph of the codebase.
    Shows which files import which, identifies hub files (imported by 5+ others),
    circular dependencies, and orphan files.
    Use this before restructuring or extracting modules.
    """
    cached = cache.get("dependency_graph")
    if cached:
        return json.dumps(cached, indent=2)
    
    graph = dep_analyzer.build_graph()
    
    # Add insights
    hubs = [n for n in graph["nodes"] if n.get("is_hub")]
    circular = graph.get("circular_dependencies", [])
    
    insights = []
    if hubs:
        hub_names = [h["id"] for h in hubs[:5]]
        insights.append(f"🔗 Hub files (high coupling): {', '.join(hub_names)}")
    if circular:
        insights.append(f"🔄 {len(circular)} circular dependency chain(s) detected — refactoring these will be complex")
    if graph.get("orphan_files"):
        insights.append(f"🌿 {len(graph['orphan_files'])} orphan files found — potential dead code or entry points")
    
    graph["insights"] = insights
    cache.set("dependency_graph", graph)
    return json.dumps(graph, indent=2)


@mcp.tool()
def get_file_dependencies(file_path: str) -> str:
    """
    Get the specific import dependencies for a single file.
    Shows what this file imports AND what imports it (reverse dependencies).
    Use this before modifying a file to understand blast radius.
    
    Args:
        file_path: Relative path to the file from the repository root
    """
    deps = dep_analyzer.get_file_dependencies(file_path)
    
    blast_radius = len(deps["imported_by"])
    deps["blast_radius_warning"] = (
        f"⚠️ Modifying this file could affect {blast_radius} other file(s)."
        if blast_radius > 0 else
        "✅ This file is not imported by others — changes are isolated."
    )
    
    return json.dumps(deps, indent=2)


@mcp.tool()
def get_module_owners() -> str:
    """
    Get ownership information for codebase modules based on git history.
    Shows which team member/email has the most commits on each file.
    Files with low bus factor (1 owner) are high knowledge-concentration risk.
    Use this to understand who to consult before changing specific areas.
    """
    cached = cache.get("owners")
    if cached:
        return json.dumps(cached, indent=2)
    
    owners = git_analyzer.get_module_owners()
    single_owner = [o for o in owners if o["bus_factor"] == 1]
    
    result = {
        "module_owners": owners[:30],
        "single_owner_risk": {
            "count": len(single_owner),
            "files": [o["path"] for o in single_owner[:10]],
            "insight": (
                f"{len(single_owner)} file(s) have only 1 contributor — "
                "these represent knowledge concentration risk (bus factor = 1)."
                if single_owner else "No single-owner files detected."
            )
        },
        "repo": REPO_PATH
    }
    
    cache.set("owners", result)
    return json.dumps(result, indent=2)


# ─── New Tools: Feature Parity with RepoWise ──────────────────────────────────

@mcp.tool()
def get_why(file_path: str) -> str:
    """
    Explain WHY a file exists and WHY it changed the way it did.
    Uses git archaeology: analyzes commit messages, PR descriptions, and inline
    decision comments (TODO, FIXME, HACK, NOTE, REASON, WORKAROUND) for this file.
    Use this when you need historical context before modifying legacy code.

    Args:
        file_path: Relative path to the file from the repository root
    """
    cached = cache.get(f"why:{file_path}")
    if cached:
        return json.dumps(cached, indent=2)

    result = why_analyzer.get_why(file_path)
    cache.set(f"why:{file_path}", result)
    return json.dumps(result, indent=2)


@mcp.tool()
def search_codebase(query: str, search_type: str = "text", file_extension: str = "") -> str:
    """
    Search across the entire codebase for a query string, pattern, or symbol name.
    Returns matching files, line numbers, and surrounding context.

    Use this to:
    - Find all usages of a function or class name: search_type="symbol"
    - Find all places a pattern occurs: search_type="text"
    - Find patterns with regex: search_type="regex"

    Args:
        query: The text, symbol name, or regex pattern to search for
        search_type: "text" (default), "symbol" (word-boundary match), or "regex"
        file_extension: Optional filter e.g. ".js" or ".py" (leave empty for all)
    """
    ext = file_extension if file_extension else None
    result = search_engine.search(
        query=query,
        search_type=search_type,
        max_results=20,
        context_lines=2,
        file_extension=ext
    )
    return json.dumps(result, indent=2)


@mcp.tool()
def find_symbol_usages(symbol_name: str) -> str:
    """
    Find where a specific function, class, or variable is defined and where it is used.
    Returns: definition location, all files that use it, and whether it might be dead code.
    More precise than search_codebase for tracking a specific named symbol.

    Args:
        symbol_name: The function, class, or variable name to look up
    """
    result = search_engine.find_symbol_usages(symbol_name)
    return json.dumps(result, indent=2)


@mcp.tool()
def get_co_change_pairs() -> str:
    """
    Find files that are ALWAYS modified together in the same commit — hidden coupling.
    These files are behaviorally coupled even if they don't import each other.
    Essential context before any refactoring: changing one will likely require changing the other.
    High coupling strength = change them together or risk breaking behavior.
    """
    cached = cache.get("co_change_pairs")
    if cached:
        return json.dumps(cached, indent=2)

    pairs = git_analyzer.get_co_change_pairs(min_co_changes=3)

    result = {
        "co_change_pairs": pairs,
        "total_pairs": len(pairs),
        "high_coupling": [p for p in pairs if p["coupling_strength"] == "HIGH"],
        "insight": (
            f"Found {len(pairs)} file pairs with hidden coupling. "
            f"{len([p for p in pairs if p['coupling_strength'] == 'HIGH'])} HIGH-strength pairs "
            f"should always be changed together."
        )
    }

    cache.set("co_change_pairs", result)
    return json.dumps(result, indent=2)


@mcp.tool()
def get_named_imports(file_path: str) -> str:
    """
    Get symbol-level import analysis for a specific file.
    Shows exactly WHICH functions/classes this file imports from other modules
    (e.g. `import { useState, useEffect } from 'react'` -> symbols: [useState, useEffect]).
    More granular than get_file_dependencies which only shows file-level imports.

    Args:
        file_path: Relative path to the file from the repository root
    """
    result = dep_analyzer.get_named_imports(file_path)
    return json.dumps(result, indent=2)


@mcp.tool()
def get_blast_radius(file_path: str) -> str:
    """
    Calculate the exact blast radius and minimal review set for a given file or list of files.
    Traces direct dependents, historical co-change partners, and associated test files.
    Returns:
    - minimal_review_set (exact files to read/review)
    - token_savings (percentage reduction & multiplier vs reading the whole codebase)

    Args:
        file_path: Target file path (or comma-separated list of paths) to analyze
    """
    result = blast_analyzer.calculate_blast_radius(file_path)
    return json.dumps(result, indent=2)


@mcp.tool()
def generate_copilot_instructions() -> str:
    """
    Analyze the codebase and generate a .github/copilot-instructions.md file.
    This file tells GitHub Copilot about:
    - High-risk files to approach carefully
    - Key architectural patterns observed
    - Modules that need extra tests
    - Dead code safe for removal
    Returns the generated markdown content and writes it to the repo.
    """
    health = health_scorer.get_summary()
    hotspots = git_analyzer.get_hotspots(5)
    dead = dead_code_detector.find_dead_code()
    graph = dep_analyzer.build_graph()
    
    worst = [f["path"] for f in health.get("worst_files", [])[:5]]
    hotspot_paths = [h["path"] for h in hotspots[:5]]
    high_dead = [d["symbol"] for d in dead if d["confidence"] == "HIGH"][:5]
    hubs = [n["id"] for n in graph["nodes"] if n.get("is_hub")][:3]
    circular = graph.get("circular_dependencies", [])
    
    avg_score = health.get("avg_score", 0)
    grade = health.get("grade", "?")
    
    instructions = f"""# GitHub Copilot Instructions — CopilotLens Analysis

*Auto-generated by CopilotLens on this repository.*

## Overall Codebase Health: {avg_score}/100 (Grade: {grade})

## ⚠️ High-Risk Files — Approach with Extra Care
These files have the highest change frequency and defect risk. **Always add/verify tests before modifying**:
{chr(10).join(f"- `{p}`" for p in hotspot_paths) if hotspot_paths else "- None detected"}

## 🏥 Lowest Health Files — Needs Refactoring Attention  
{chr(10).join(f"- `{p}`" for p in worst) if worst else "- None detected"}

## 🔗 Hub Files — High Blast Radius
Changes to these files affect many others:
{chr(10).join(f"- `{p}`" for p in hubs) if hubs else "- None detected"}

## 🔄 Circular Dependencies
{f"⚠️ {len(circular)} circular dependency chain(s) exist. Avoid deepening these patterns." if circular else "✅ No circular dependencies detected."}

## 🧹 Dead Code Candidates (Safe to Remove)
{chr(10).join(f"- `{s}`" for s in high_dead) if high_dead else "- None detected with high confidence"}

## 📋 Coding Guidelines for this Codebase
- Run tests after any change to high-risk files listed above
- Prefer small, focused functions (< 50 lines)
- Add docstrings to any public functions in hub files
- Do not introduce new circular dependencies
- Check module ownership before proposing architecture changes

## 🛠️ CopilotLens MCP Tools Available
Use these tools in Agent mode for deeper analysis:
- `get_file_health(path)` — score any file before editing
- `get_hotspots()` — see the riskiest files
- `get_dependency_graph()` — understand blast radius
- `get_dead_code()` — find safe cleanup opportunities
"""
    
    # Write to .github/copilot-instructions.md
    output_path = Path(REPO_PATH) / ".github" / "copilot-instructions.md"
    try:
        output_path.parent.mkdir(parents=True, exist_ok=True)
        output_path.write_text(instructions, encoding="utf-8")
        write_status = f"✅ Written to: {output_path}"
    except Exception as e:
        write_status = f"⚠️ Could not write file: {e}"
    
    return json.dumps({
        "content": instructions,
        "write_status": write_status,
        "path": str(output_path)
    }, indent=2)


@mcp.tool()
def get_dashboard_url() -> str:
    """
    Get the URL to the CopilotLens visual dashboard.
    The dashboard shows codebase health, hotspots heatmap, and dependency graphs in a browser.
    """
    return json.dumps({
        "dashboard_url": f"http://localhost:{DASHBOARD_PORT}",
        "status": "Open this URL in your browser to see the visual dashboard"
    })


# ─── Dashboard HTTP Server ─────────────────────────────────────────────────────

def get_dashboard_data() -> dict:
    """Collect all analysis data for the dashboard."""
    try:
        health = health_scorer.get_summary()
    except Exception:
        health = {"avg_score": 0, "distribution": {}, "worst_files": [], "best_files": []}
    
    try:
        hotspots = git_analyzer.get_hotspots(15)
    except Exception:
        hotspots = []
    
    try:
        dead = dead_code_detector.find_dead_code()
    except Exception:
        dead = []
    
    try:
        graph = dep_analyzer.build_graph()
    except Exception:
        graph = {"nodes": [], "edges": [], "circular_dependencies": [], "orphan_files": []}
    
    try:
        repo_summary = git_analyzer.get_repo_summary()
    except Exception:
        repo_summary = {}

    return {
        "repo_path": REPO_PATH,
        "health": health,
        "hotspots": hotspots,
        "dead_code": dead[:20],
        "dependency_graph": {
            "nodes": graph["nodes"][:50],
            "edges": graph["edges"][:100],
            "circular": graph.get("circular_dependencies", []),
            "orphans": graph.get("orphan_files", [])[:10]
        },
        "repo_summary": repo_summary
    }


class DashboardHandler(BaseHTTPRequestHandler):
    def log_message(self, format, *args):
        pass  # Suppress default HTTP logs

    def do_POST(self):
        if self.path == "/api/sync-instructions":
            result = generate_copilot_instructions()
            body = result.encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Access-Control-Allow-Origin", "*")
            self.send_header("Content-Length", len(body))
            self.end_headers()
            self.wfile.write(body)
        else:
            self._send_text(404, "Not found")

    def do_GET(self):
        next_out_dir = Path(__file__).parent.parent / "dashboard-next" / "out"
        dashboard_dir = next_out_dir if next_out_dir.exists() else (Path(__file__).parent.parent / "dashboard")

        if self.path == "/api/data":
            data = get_dashboard_data()
            body = json.dumps(data).encode()
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Access-Control-Allow-Origin", "*")
            self.send_header("Content-Length", len(body))
            self.end_headers()
            self.wfile.write(body)
        elif self.path == "/api/sync-instructions":
            result = generate_copilot_instructions()
            body = result.encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Access-Control-Allow-Origin", "*")
            self.send_header("Content-Length", len(body))
            self.end_headers()
            self.wfile.write(body)
        else:
            rel_path = self.path.lstrip("/")
            if not rel_path or rel_path == "index.html":
                file_path = dashboard_dir / "index.html"
            else:
                file_path = dashboard_dir / rel_path

            if file_path.exists() and file_path.is_file():
                content_type = "text/html; charset=utf-8"
                if file_path.suffix == ".css":
                    content_type = "text/css"
                elif file_path.suffix == ".js":
                    content_type = "application/javascript"
                elif file_path.suffix == ".json":
                    content_type = "application/json"
                elif file_path.suffix == ".svg":
                    content_type = "image/svg+xml"

                body = file_path.read_bytes()
                self.send_response(200)
                self.send_header("Content-Type", content_type)
                self.send_header("Access-Control-Allow-Origin", "*")
                self.send_header("Content-Length", len(body))
                self.end_headers()
                self.wfile.write(body)
            else:
                self._send_text(404, "Not found")

    def _send_text(self, code, msg):
        body = msg.encode()
        self.send_response(code)
        self.send_header("Content-Type", "text/plain")
        self.send_header("Content-Length", len(body))
        self.end_headers()
        self.wfile.write(body)


def start_dashboard():
    """Start the dashboard HTTP server in a background thread."""
    try:
        server = HTTPServer(("localhost", DASHBOARD_PORT), DashboardHandler)
        print(f"[CopilotLens] Dashboard: http://localhost:{DASHBOARD_PORT}", file=sys.stderr, flush=True)
        server.serve_forever()
    except OSError as e:
        print(f"[CopilotLens] Dashboard could not start on port {DASHBOARD_PORT}: {e}", file=sys.stderr, flush=True)


# ─── Helpers ───────────────────────────────────────────────────────────────────

def _interpret_summary(health: dict) -> str:
    avg = health.get("avg_score", 0)
    dist = health.get("distribution", {})
    critical = dist.get("critical", 0)
    total = health.get("total_files", 0)
    
    if avg >= 80:
        return f"✅ Codebase is in excellent health ({avg}/100). {critical} critical files need attention."
    elif avg >= 65:
        return f"👍 Codebase health is good ({avg}/100). Focus on the {critical} critical files."
    elif avg >= 50:
        return f"⚠️ Codebase health is fair ({avg}/100). {critical}/{total} files are in critical state."
    else:
        return f"🚨 Codebase health is poor ({avg}/100). Major refactoring recommended. {critical}/{total} files critical."


# ─── Entry Point ───────────────────────────────────────────────────────────────

if __name__ == "__main__":
    print(f"[CopilotLens] Starting MCP server for repo: {REPO_PATH}", file=sys.stderr, flush=True)
    
    # Start dashboard in background thread
    dashboard_thread = threading.Thread(target=start_dashboard, daemon=True)
    dashboard_thread.start()
    
    # Run MCP server (blocking)
    mcp.run(transport="stdio")
