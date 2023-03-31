import React from "react";
import PropTypes from "prop-types";
import ReactDOM from "react-dom";
import {Button, FormControl, FormGroup, OverlayTrigger, Tooltip} from "react-bootstrap";
import * as Constants from "../../../../common/constants.js";

class RelationSelectionMenu extends React.Component {

    constructor(props) {
        super(props);

        this.parseSelectedOption = this.parseSelectedOption.bind(this);
        this.renderOption = this.renderOption.bind(this);
        
        let outgoingRelations = this.props.relationOptions.filter(rel => rel["direction"] === "outgoing")
                .sort((a, b) => a["label"].localeCompare(b["label"]));
        //console.log("outgoingRelations: ", outgoingRelations);
        
        let incomingRelations = this.props.relationOptions.filter(rel => rel["direction"] === "incoming")
                .sort((a, b) => a["label"].localeCompare(b["label"]));
        //console.log("incomingRelations: ", incomingRelations);
        
        /*
        let selection = "";
        
        if (outgoingRelations[0]) {
            selection = outgoingRelations[0]["type"];
            console.log("select outgoing: " + selection);
        }
        else if (incomingRelations[0]) {
            selection = incomingRelations[0]["type"];            
            console.log("select incoming: " + selection);
        }
        */
        
        let selection = 0; //now we simply use numeric values
        
        let options = outgoingRelations.concat(incomingRelations);
        //console.log("options:", options);
        
        this.state = {
            outgoingRelations: outgoingRelations,
            incomingRelations: incomingRelations,
            options: options,
            selection: selection
        };
        
        //console.log("initial state:", this.state);
    }

    render() {
        //let relations = this.state.outgoingRelations.concat(this.state.incomingRelations);
        let options = this.state.options;
        
        return (
            <div ref="select-menu" className="popup-menu">
                <div className="header">
                    <h1>Select Relation</h1>
                    <span classID="rel-delete" className="menu-close fa fa-times"
                          onClick={this.props.closeMenu}/>
                </div>
                <div className="content">
                    <div className="options">
                        <FormGroup controlId="formControlsSelect">
                            <FormControl ref="selection" componentClass="select" placeholder="Select" 
                                         size={options.length}
                                         value={this.state.selection}
                                         autoFocus={true}
                                         onChange={(e) => this.setState({...this.state, selection: e.nativeEvent.target.value})}
                            >
                                {options.map((option, index) => this.renderOption(option, index))}
                            </FormControl>
                        </FormGroup>
                        <form>
                        </form>
                    </div>
                    <hr/>
                </div>
                <div className="footer">
                    <Button bsStyle="primary" bsSize="small" disabled={false} onClick={this.parseSelectedOption}>
                        Select
                    </Button>
                </div>
            </div>
        );
    }
    
    renderOption(option, index) {
        //let label = option["label"] + " (" + option["direction"] + ")"; //show label with direction
        let label = option["from"] + " " + option["label"] + " " + option["to"]; //show label with asset names (labels)
        //console.log(index, option);
        let value = index;
        if (option["comment"].length && option["comment"].length > 0) {
            let props = {
                delayShow: Constants.TOOLTIP_DELAY, placement: "right",
                key: "ot-" + option["label"] + "-" + option["direction"], defaultOverlayShown: false,
                overlay: <Tooltip id={"ot-" + option["label"] + "-" + option["direction"] + "-tooltip"}
                                  className={"tooltip-overlay"}>
                    {option["comment"]}
                </Tooltip>
            };
            return <OverlayTrigger {...props}>
                <option key={"option-" + option["label"] + "-" + option["direction"]}
                        value={value} data-direction={option["direction"]}>{label}</option>
            </OverlayTrigger>;
        }
        else return "";
    }

    parseSelectedOption() {
        let selectedOption = ReactDOM.findDOMNode(this.refs["selection"]);
        console.log("selectedOption", selectedOption);
        let index = selectedOption.value;
        let option = this.state.options[index];
        let value = option;
        //console.log("value", value);
        //let direction = $('option:selected').data('direction');
        let direction = option.direction;
        console.log("direction", direction);
        this.props.selectOption(value, direction, this.props.updating, this.props.relation);
    }
}

/**
 * This describes the data types of all of the props for validation.
 * @type {{onRelationCreation: *, asset: *, assetType: *, modelId: *, dispatch: *}}
 */
RelationSelectionMenu.propTypes = {
    updating: PropTypes.bool,
    relationOptions: PropTypes.array,
    relation: PropTypes.object,
    selectOption: PropTypes.func,
    closeMenu: PropTypes.func
};

/* This exports the AssertedAsset class as required. */
export default RelationSelectionMenu;
