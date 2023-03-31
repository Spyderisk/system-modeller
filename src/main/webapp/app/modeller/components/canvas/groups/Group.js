import React from "react";
import PropTypes from "prop-types";
import ReactDOM from "react-dom";
import {Rnd} from "react-rnd";
import * as Input from "../../../../common/input/input.js";
import GroupCtxMenu from "../popups/GroupCtxMenu"
import CollapseGroupGlyph from "./glyphs/CollapseGroupGlyph";
import DeleteTileGlyph from "../assets/glyphs/DeleteAssertedAssetGlyph";
import {
    putGroupExpanded,
    putAssertedGroupRename,
    putAssertedGroupResize,
} from "../../../actions/ModellerActions";
import {
    _delEndpoints,
    connectorPaintStyle,
    dragDotOver,
    draggingConnection,
    endpointHoverStyle,
    getCurrentEndpointIds,
    getCurrentEndpoints,
    getPlumbingInstance,
    getStartElementId,
    isConnStarted,
    outerOver,
    setDragDotOver,
    setDraggingConnection,
    setLinkHover,
    setOuterOver,

} from "../../util/TileFactory";
import '../../../css/assertedAsset.scss';
import * as Constants from "../../../../common/constants.js"
import EditAssetTypeModal from "../../panes/details/popups/EditAssetTypeModal";

/**
 * This component represents a group on the canvas within which asserted assets can be placed.
 */
class Group extends React.Component {


    /**
     * This constructor is used to bind methods to this class.
     * @param props Props passed from canvas.
     */
    constructor(props) {
        super(props);
        //console.log("Group: constructor: ", props);

        this.addGroupAssets = this.addGroupAssets.bind(this);
        this.renderGroup = this.renderGroup.bind(this);
        this.handleResize = this.handleResize.bind(this);
        this.handleCollapse = this.handleCollapse.bind(this);
        this.handleEditLabel = this.handleEditLabel.bind(this);
        this.handleGroupContextMenu = this.handleGroupContextMenu.bind(this);
        this.groupContextTrigger = this.groupContextTrigger.bind(this);
        this.groupContextTriggerVar = null;

        this.state = {
            loading: false,
            timeout: false,
            isCollapsed: !props.group.expanded
        };
    }

    /**
     * React Lifecycle Methods
     */

    /**
     * This lifecycle method is called once the group component has been rendered.
     * Here we are ensuring that the group renders in the correct position (this must be done post render so that we
     * can know the exact dimensions of the DIV on the document.
     */
    componentDidMount() {
        this.$el = $(this.el);
        //console.log("Group: componentDidMount: this.$el", this.$el);
        if (!this.$el) return;
        
        let groupEl = this.$el[0];
        //console.log("Group: componentDidMount: group element", groupEl);

        let groupId = this.props.group.id;
        let groupUri = this.props.group.uri;
        
        let jsplumb = getPlumbingInstance();

        //console.log("Group: addGroup: ", groupId, this.props.group);
        jsplumb.addGroup({
            el: groupEl,
            id: groupId,
            collapsed: !this.props.group.expanded,
            droppable: true,
            orphan: true,
            constrain: false,
            draggable: true,
            dragOptions: {
                stop: (params) => {
                    console.log("group draggable stop params: ", params);
                    const newX = params.pos[0];
                    const newY = params.pos[1];

                    const clickedGroup = params.e.target.parentNode;

                    this.props.handleGroupDrag(params.e, newX, newY, groupId, groupUri);
                }
            }
        });
        
        this.addGroupAssets();
        
        //Add right-click context menu handler for group
        //N.B. comment out this line to disable menu (if required for dev purposes)
        this.$el.on("contextmenu", (event) => {
            this.handleGroupContextMenu(event);
        });
    }

    componentDidUpdate(prevProps, prevState) {
        //Here, we ensure that the Rnd element is at the same position as the Group
        //e.g. if the group has been dragged to a new location
        let x = parseInt(this.props.group.left.replace("px", ""));
        let y = parseInt(this.props.group.top.replace("px", ""));
        //console.log("Group: componentDidUpdate: updating rnd location to ", this.props.group.left, this.props.group.top);
        this.rnd.updatePosition({ x: x, y: y });
        
        this.addGroupAssets();
    }
    
    addGroupAssets() {
        let groupId = this.props.group.id;
        let jsplumb = getPlumbingInstance();

        this.props.group.assetIds.map((assetId) => {
            let tileId = "#tile-" + assetId;
            //console.log("Group: addToGroup tileId: ", tileId);
            let el = $(tileId)[0];
            //console.log("Group: addToGroup: ", groupId, el);
            if (el) {
                /* Here, we set the doNotFireEvent flag to true, as we don't need to notify that
                 * this has happened. N.B. the jsplumb group.add method has been modified
                 * to only add asset elements to the group elements array when this flag is
                 * set, avoiding elements from being added multiple times. See comment in jsplumb.js
                 */
                jsplumb.addToGroup(groupId, el, true); //do not fire event
            }
            else {
                console.warn("Cannot locate asset element: " + tileId);
            }
        });
        
        let jsGroup = jsplumb.getGroup(groupId);
        let members = jsGroup.getMembers();
        //console.log("Group: members: ", members);
    }
    
    componentDidCatch(error, errorInfo) {
        console.log("Group: componentDidCatch for group: " + this.props.group.label, error, errorInfo);
        alert("ERROR rendering group \"" + this.props.group.label + "\": Please refresh the page!");
    }

    static getDerivedStateFromError(error) {
        console.log("Group: getDerivedStateFromError for group: " + error);
        return { hasError: true };
    }
    
    /**
     * This method renders the group, with all of its sub components, to the tile canvas.
     * @returns {XML}
     */
    render() {
        //console.log("Group: render: ", this.props.group);
        //console.log("Group: state.isCollapsed: ", this.state.isCollapsed);
        
        let x = parseInt(this.props.group.left.replace("px", ""));
        let y = parseInt(this.props.group.top.replace("px", ""));
        let width = this.props.group.width;
        let height = this.props.group.height;
        //console.log("Group: x, y: ", x, y);

        return (
            <div>
            <Rnd ref={ c => { this.rnd = c; } }
                 default={{
                     x: x,
                     y: y,
                     width: width,
                     height: height
                 }}
                 minWidth={100}
                 minHeight={100}
                 disableDragging={true}
                 onDragStop={(e, d) => {
                     //console.log("Group Rnd onDragStop(): e, d:", e, d);
                     this.handleDrag(e, d.x, d.y);
                 }}
                 onResizeStart={(e, direction, ref) => {
                    console.log("Group onResizeStart");
                    //locate the main grouop element
                    let id="#tile-" + this.props.group["id"];
                    //add the resizing class (dashed border)
                    $(id).addClass("resizing");
                 }}
                 onResize={(e, direction, ref, delta, position) => {
                    //console.log("Group Rnd resize to width, height: ", ref.style.width, ref.style.height);
                    let width = ref.style.width.replace("px", "");
                    let height = ref.style.height.replace("px", "");
                    //locate the main grouop element
                    let id="#tile-" + this.props.group["id"];
                    //console.log($(id)[0].style);
                    //adjust the width/height of the group element to match that of the Rnd element
                    $(id).css({"height": height, "width": width});
                 }}
                 onResizeStop={(e, direction, ref, delta, position) => {
                    //first remove the resizing class (dashed border)
                    let id="#tile-" + this.props.group["id"];
                    $(id).removeClass("resizing");
                    
                    console.log("Group Rnd resized to width, height: ", ref.style.width, ref.style.height);
                    let width = ref.style.width.replace("px", "");
                    let height = ref.style.height.replace("px", "");
                    //send the new group size
                    this.handleResize(width, height);
                 }}
            >
            </Rnd>
            {this.renderGroup()}
            </div>
        );
    }
    
    renderGroup() {
        let hasError = this.state.hasError;
        //console.log("renderGroup: isCollapsed = ", this.state.isCollapsed);
        
        if (hasError) {
            console.warn("Group has error: " + this.props.group.label);
        }
        
        let className = "group group-container" 
                + (hasError ? " group-error jtk-group-expanded" : "")
                + (this.state.isCollapsed ? " jtk-group-collapsed" : " jtk-group-expanded");
        
        //console.log("renderGroup: className = " + className);
        //console.log("renderGroup: assets = ", this.props.group.assetIds);
        
        let minWidth = !this.state.isCollapsed ? this.props.group.width : null;
        let minHeight = !this.state.isCollapsed ? this.props.group.height : null;
        
        let styles = {
            backgroundColor: "WhiteSmoke",
            left: this.props.group.left,
            top: this.props.group.top,
            position: "absolute",
            height: minHeight,
            width: minWidth
        };
        
        
        let groupBodyStyles = {
            height: "calc(100% - 36px)",
            width: "100%"
        };
        
        return <div
                  id={"tile-" + this.props.group["id"]}
                  key={"group-key-" + this.props.group["id"]}
                  style={styles} ref={el => {this.el = el;}}
                  className={className} group={this.props.group["id"]}>
                     <div className="glyph-bar">
                         <div className="glyphs">
                             <CollapseGroupGlyph
                                 isCollapsed={this.state.isCollapsed}
                                 groupId={this.props.group.id}
                                 onClick={this.handleCollapse}/>
                             <span style={{width: '100%'}}></span>
                             <DeleteTileGlyph
                                 groupId={this.props.group.id}
                                 onDelClick={this.handleDelete}/>
                         </div>
                     </div>
                     <div id={"group-body-" + this.props.group.id} className="group-body" style={groupBodyStyles}/>
                     {!hasError && this.props.group.assetIds.map((assetId) => {
                        return this.props.renderGroupAsset(assetId);
                     })}
                     <label onClick={this.handleEditLabel} className="tile-label text-primary title-change">{this.props.group.label}</label>
            </div>
    }

    handleGroupContextMenu(event) {
        event.preventDefault();

        if (!document.getElementById("group-context-menu")) {
            let menu = document.createElement("div");
            document.body.appendChild(menu);
            $(menu).attr("id", "group-context-menu");
        }
        ReactDOM.render(<GroupCtxMenu
                modelId={ this.props.modelId }
                group={ this.props.group }
                deleteGroup={ this.props.deleteGroup }
                contextTrigger={ this.groupContextTrigger }
                />,
            document.getElementById("group-context-menu"));

        if (this.groupContextTriggerVar) {
            this.groupContextTriggerVar.handleContextClick(event);
        }
    }
    
    groupContextTrigger(c) {
        //console.log("groupContextTrigger: ", c);
        this.groupContextTriggerVar = c;
    }
    
    handleCollapse() {
        let currCollapsed = this.state.isCollapsed;
        
        this.setState(state => ({
           isCollapsed: !state.isCollapsed
        }));
        
        let expanded;
        
        if (currCollapsed) {
            expanded = true;
            this.props.expandGroup(this.props.group.id);
        } else {
            expanded = false;
            this.props.collapseGroup(this.props.group.id);
        }
        
        let updatedGroup = {
            id: this.props.group.id,
            uri: this.props.group.uri,
            expanded: expanded
        };
        
        //update expanded state on server
        this.props.dispatch(putGroupExpanded(this.props.modelId, updatedGroup));
    }
    
    handleDrag(e, newX, newY) {
        //console.log("handleDrag: ", newX, newY);
        let groupId = this.props.group.id;
        let groupUri = this.props.group.uri;
        this.props.handleGroupDrag(e, newX, newY, groupId, groupUri);
    }
    
    handleResize(width, height) {
        //console.log("handleResize: ", width, height);
        
        let updatedGroup = {
            id: this.props.group.id,
            uri: this.props.group.uri,
            width: width,
            height: height
        };
        
        console.log("Calling putAssertedGroupResize: ", updatedGroup);
        this.props.dispatch(putAssertedGroupResize(this.props.modelId, updatedGroup));
    }
    
    /**
     * This method is called when the group name is clicked.
     * @param event The "click" event that is fired.
     */
    handleEditLabel(event) {
        console.log("handleEditLabel");
        
        const self = this;
        const label = $(event.target);
        console.log(label);
        label.after("<input id='group-rename' type='text' class='text-primary' maxLength=" + Constants.MAX_ASSET_NAME_LENGTH + "/>");
        const textBox = label.next();
        label.hide();
        textBox.show();
        textBox.val(Input.unescapeString(label.html()));
        textBox.mouseup(() => false);
        textBox.focus();
        textBox.on("blur", () => {
            let name = this.props.group.label;
            let newName = textBox.val();

            if (newName === "") {
                console.log("WARNING: empty name");
            }
            else if (name === newName) {
                console.log("WARNING: name has not changed");
            }
            else if (self.props.isGroupNameTaken(name, newName)) {
                console.log("Renaming group to:", newName);
                this.props.dispatch(putAssertedGroupRename(
                    this.props.modelId,
                    {
                        ...this.props.group,
                        label: newName
                    }
                ));
                label.html(Input.escapeString(newName));
            } else {
                alert("That name has already been taken!");
            }

            label.show();
            $(document).off("click");
            textBox.remove();
        });
        textBox.on("keyup",
            (e) => {
                //console.log("keyup: ", e.keyCode);
                if (e.keyCode === 13) {
                    textBox.blur();
                }
            });
        $(document).on("click",
            (e) => {
                if (e.target.id !== textBox.attr("id") && textBox.is(":focus")) {
                    //console.log("calling blur()");
                    textBox.blur();
                }
            });
    }

}

Group.propTypes = {
    group: PropTypes.object,
    modelId: PropTypes.string,
    isGroupNameTaken: PropTypes.func,
    renderGroupAsset: PropTypes.func,
    handleGroupDrag: PropTypes.func,
    expandGroup: PropTypes.func,
    collapseGroup: PropTypes.func,
    deleteGroup: PropTypes.func,
    dispatch: PropTypes.func
};

export default Group;
