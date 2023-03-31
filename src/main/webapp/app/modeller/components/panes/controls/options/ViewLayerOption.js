import React from "react";
import PropTypes from 'prop-types';

import {updateLayerSelection} from "../../../../../modeller/actions/ModellerActions";
import {Button, OverlayTrigger, Popover, Well} from "react-bootstrap";
import * as Constants from "../../../../../common/constants.js";

class ViewLayerOption extends React.Component {

    constructor(props) {
        super(props);

        this.handleOptionChange = this.handleOptionChange.bind(this);
        this.handleSubmit = this.handleSubmit.bind(this);
    }

    componentWillMount() {
        this.setState({
            selectedOptions: this.props.selectedLayers
        })
    }

    render() {
        return (
            <Well className="option-well">
                <h4>{"Select View Layer"}</h4>
                <hr />
                <div>
                    <form className="option-block">

                        <label>
                            <input type="radio" value="all"
                                   checked={this.state.selectedOptions.indexOf("all") > -1}
                                   onChange={this.handleOptionChange} />
                            <OverlayTrigger delayShow={Constants.TOOLTIP_DELAY} placement="right"
                                            overlay={<Popover id="single-layer-popover"
                                                title={'All - Layer Details'}>
                                                <strong>{"Showing everything"}</strong>
                                            </Popover>}>
                                <span>&nbsp;{'All'}</span>
                            </OverlayTrigger>
                        </label>

                        {
                            this.props.layers !== undefined && this.props.layers.map((layer) => {
                                return <label style={{"display": "block"}}>
                                    <input type="radio" value={layer.name}
                                           checked={this.state.selectedOptions.indexOf(layer['name']) > -1}
                                           onChange={this.handleOptionChange} />
                                    &nbsp;{layer.name}
                                </label>
                            })
                        }
                    </form>
                </div>
                <hr />
                {
                    this.state.selectedOptions.length > 0 ?
                        <Button bsStyle="primary" onClick={this.handleSubmit}>Apply</Button>
                        :
                        <Button disabled bsStyle="primary" onClick={this.handleSubmit}>Apply</Button>
                }
            </Well>
        );
    }

    //Updates the state when a change is made to the options
    handleOptionChange(event) {
        this.setState({
            ...this.state,
            selectedOptions:[event.target.value]
        });
    }

    //Dispatches the layer update function with the stated options
    handleSubmit(){
        var selection = this.state.selectedOptions;
        this.props.dispatch(updateLayerSelection(selection));
    }

}

ViewLayerOption.propTypes = {
    modelId: PropTypes.string,
    layers: PropTypes.array,
    selectedLayers: PropTypes.array,
    dispatch: PropTypes.func,
};

export default ViewLayerOption;
