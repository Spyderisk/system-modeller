import React from "react";
import PropTypes from "prop-types";
import ReactDOM from "react-dom";
import {FormGroup, FormControl, Button} from "react-bootstrap";

class InferredAssetSelectionMenu extends React.Component {

    constructor(props) {
        super(props);

        this.parseSelectedOption = this.parseSelectedOption.bind(this);
    }


    render() {
        return (
            <div ref="select-menu" className="popup-menu">
                <div className="header">
                    <h1>Select Inferred Asset</h1>
                    <span classID="rel-delete" className="menu-close fa fa-times"
                          onClick={this.props.closeMenu}/>
                </div>
                <div className="content">
                    <div className="options">
                        <FormGroup controlId="formControlsSelect">
                            <FormControl ref="selection" componentClass="select" placeholder="Select">
                                {this.props.inferredAssetOptions.sort((a, b) => a["label"].localeCompare(b["label"]))
                                    .map((option) => <option value={option["id"]}
                                                             selected={option["selected"]}>{option["label"]}</option>)}
                            </FormControl>
                        </FormGroup>
                        <form>
                        </form>
                    </div>
                    <hr/>
                </div>
                <div className="footer">
                    <Button bsStyle="primary" bsSize="small" onClick={this.parseSelectedOption}>
                        Select
                    </Button>
                </div>
            </div>
        );
    }

    parseSelectedOption() {
        this.props.selectOption(ReactDOM.findDOMNode(this.refs["selection"]).value);
    }
}

/**
 * This describes the data types of all of the props for validation.
 * @type {{onRelationCreation: *, asset: *, assetType: *, modelId: *, dispatch: *}}
 */
InferredAssetSelectionMenu.propTypes = {
    inferredAssetOptions: PropTypes.array,
    selectOption: PropTypes.func,
    closeMenu: PropTypes.func
};

/* This exports the InferredAssetSelectionMenu class as required. */
export default InferredAssetSelectionMenu;
