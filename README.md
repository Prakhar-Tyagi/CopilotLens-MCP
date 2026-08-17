# CopilotLens 🔍
### Codebase Intelligence for GitHub Copilot in IntelliJ

> *"What if GitHub Copilot knew everything a senior engineer knows about your codebase — before it even suggested a change?"*

CopilotLens is an MCP (Model Context Protocol) server that gives GitHub Copilot's Agent mode deep, structural knowledge about any codebase: health scores, hotspots, dead code, dependency graphs, and module ownership — all surfaced directly into your Copilot conversations.

---

## ✅ Proof It Works (Local Test)

```
=== CopilotLens Functional Test ===
Health Summary: avg=48, grade=D, files=9
Worst file: mcp_server/server.py score=15
Git: 3 commits, 10 files changed
Dependency graph: 9 nodes, 9 edges
Dead code candidates: 2
Example: copyPrompt in dashboard/app.js
=== ALL TESTS PASSED ===
```

---

## 🚀 Quick Start (Any Machine)

### Step 1: Install Python 3.9+
```
https://python.org/downloads/
```

### Step 2: Clone / Copy this folder
Copy the entire `Siemens Hack` project folder to your machine.

### Step 3: Install dependencies
```bash
cd "Siemens Hack"
pip install mcp gitpython
```

### Step 4: Run the server on ANY repository
```bash
python mcp_server/server.py --repo /path/to/your/project
```

The server starts and prints:
```
[CopilotLens] Starting MCP server for repo: /path/to/your/project
[CopilotLens] Dashboard: http://localhost:8765
```

### Step 5: Open the dashboard
Open your browser to: **http://localhost:8765**

You'll see:
- 🏥 Overall codebase health score + grade
- 🔥 Top hotspot files (highest churn)
- 🧹 Dead code candidates
- 🔗 Dependency graph analysis
- 👥 Module ownership

---

## 🖥️ IntelliJ + GitHub Copilot Setup (Work Laptop)

### Prerequisites
- IntelliJ IDEA 2024.3+ (any edition)
- GitHub Copilot plugin installed and active
- Python 3.9+ on PATH

### Step 1: Copy the project
Copy this entire folder to your work machine (USB, Teams, email, etc.)

### Step 2: Install dependencies on work machine
```bash
pip install mcp gitpython
```

### Step 3: Configure IntelliJ MCP

**Option A: Project-level (recommended)**  
Create or edit `.github/copilot-instructions.md` in your target project:
```json
// Also create: ~/.config/github-copilot/intellij/mcp.json
```

**Option B: Global config**  
Edit the file at:
```
~/.config/github-copilot/intellij/mcp.json
```
(On Windows: `%USERPROFILE%\.config\github-copilot\intellij\mcp.json`)

Use this content (replace the path):
```json
{
  "servers": {
    "copilotlens": {
      "command": "python",
      "args": [
        "C:/path/to/Siemens Hack/mcp_server/server.py",
        "--repo",
        "${workspaceFolder}"
      ],
      "env": {
        "COPILOTLENS_PORT": "8765"
      }
    }
  }
}
```

### Step 4: Enable in IntelliJ
1. Open IntelliJ → GitHub Copilot icon (status bar)
2. Click **Edit Settings** → **Model Context Protocol (MCP)**
3. Verify `copilotlens` server is listed
4. Open Copilot Chat → switch to **Agent mode**
5. Click the 🔧 **Tools** icon → enable CopilotLens tools

### Step 5: Test it
In Copilot Agent chat, type:
```
What are the top hotspots in this codebase?
```
Copilot will call `get_hotspots()` and return real data from your codebase!

---

## 🛠️ MCP Tools Reference

| Tool | What it does |
|------|-------------|
| `get_file_health(path)` | Score a file 0–100, get breakdown of issues |
| `get_hotspots(n)` | Top N high-churn, high-risk files from git history |
| `get_codebase_summary()` | Overall health grade + distribution + git stats |
| `get_dead_code()` | Unused functions/classes — safe cleanup targets |
| `get_dependency_graph()` | Import map: hubs, circular deps, orphans |
| `get_file_dependencies(path)` | What imports this file + what this file imports |
| `get_module_owners()` | Ownership from git: bus factor per file |
| `generate_copilot_instructions()` | Auto-writes `.github/copilot-instructions.md` |
| `get_dashboard_url()` | Returns the dashboard URL |

---

## 🎯 Demo Flow for Judges

1. **Open browser** → `http://localhost:8765` → show the dashboard (30 sec)
2. **Open IntelliJ** with a large project
3. **Open Copilot Agent mode**
4. Ask: *"What are the riskiest files in this codebase before I start refactoring?"*
5. Copilot calls `get_hotspots()` → returns real churn data with risk levels
6. Ask: *"Generate the Copilot instructions file for this project"*
7. Copilot calls `generate_copilot_instructions()` → writes `.github/copilot-instructions.md`
8. Show the generated file — Copilot now has deep codebase context forever

**The before/after moment:**
> Without CopilotLens: *"Here's a generic refactoring plan"*  
> With CopilotLens: *"File X scores 15/100, is the #1 hotspot with 47 commits in 90 days, owned by one person (bus factor = 1). Here's a targeted, risk-aware plan."*

---

## 📁 Project Structure

```
Siemens Hack/
├── hack.md                          # Hackathon reference notes
├── test_analyzers.py                # Proof-it-works test script
├── mcp_server/
│   ├── server.py                    # Main MCP server (8 tools + dashboard)
│   ├── requirements.txt
│   └── analyzers/
│       ├── git_analyzer.py          # Git history: churn, hotspots, ownership
│       ├── code_health.py           # Scoring engine (12 markers, 0-100)
│       ├── dependency.py            # Import graph analysis
│       ├── dead_code.py             # Unused symbol detection
│       └── cache.py                 # JSON cache layer
├── dashboard/
│   ├── index.html                   # Visual dashboard
│   ├── style.css                    # Dark mode premium design
│   └── app.js                       # Charts + live data rendering
└── intellij_setup/
    └── mcp.json                     # IntelliJ MCP config template
```

---

## ⚡ Tips for a Massive Codebase

If your codebase has 10,000+ files:
- The health scorer limits to 200 files by default (fastest files)
- The dependency graph limits to 300 files
- Git analysis is fast regardless (uses git CLI directly)
- First analysis takes ~30-60 seconds; subsequent calls hit cache (< 1s)

To increase limits, edit `limit=200` in `server.py` calls.

---

## 🔧 Troubleshooting

| Problem | Fix |
|---------|-----|
| `ModuleNotFoundError: mcp` | Run `pip install mcp` |
| `ModuleNotFoundError: git` | Run `pip install gitpython` |
| Dashboard not loading | Check port 8765 is free; try `COPILOTLENS_PORT=8766 python server.py ...` |
| No git data | Ensure the target repo has git history (run `git log` to verify) |
| Copilot doesn't show tools | Restart IntelliJ after saving mcp.json; ensure Agent mode is selected |
| MCP server not detected | Check mcp.json path is correct; look in IntelliJ Copilot logs |
