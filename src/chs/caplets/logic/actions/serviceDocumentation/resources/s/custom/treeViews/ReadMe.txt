The folder contains scripts/styles and images to change system's view in navigation panel from flat list to a tree list.
Following steps will convert a flat system view to a tree view.
1) Copy all images to images folder under Packets directory.
2) Append content of custom.css to custom.css file in the Packets directory.
3) Copy s/systemsAsTreeView.js and s/treeView.html to Packets/s folder
4) Change system's view in Packets/config.xml as follows

from
<component name="system" view="views/systems" template="templates/flatListTemplate.html"
               container="#platform-grouped-list>div>.system"/>
               
to                             

<component name="system" view="systemsAsTreeView" template="treeView.html"
               container="#platform-grouped-list>div>.system"/>
