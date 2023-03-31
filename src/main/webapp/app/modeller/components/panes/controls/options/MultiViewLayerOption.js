import React from "react";
import PropTypes from 'prop-types';

import {updateLayerSelection} from "../../../../../modeller/actions/ModellerActions";
import {Button, OverlayTrigger, Popover, Well} from "react-bootstrap";
import * as Constants from "../../../../../common/constants.js";

class MultiViewLayerOption extends React.Component {

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
                            <input type="checkbox" value="all"
                                   checked={this.state.selectedOptions.indexOf("all") > -1}
                                   onChange={this.handleOptionChange} />
                            <OverlayTrigger delayShow={Constants.TOOLTIP_DELAY} placement="right"
                                            overlay={<Popover id="multi-layers-popover"
                                                title={'All - Layer Details'}>
                                                <strong>{"Showing everything"}</strong>
                                            </Popover>}>
                                <span>{'All'}</span>
                            </OverlayTrigger>
                        </label>

                        {
                            this.props.layers.map((layer) => {
                                var layerName = layer['name'].replace(/([A-Z])/g, ' $1').replace((/^./, (str) => str.toUpperCase()));
                                return <label>
                                    <input type="checkbox" value={layer['name']}
                                           checked={this.state.selectedOptions.indexOf(layer['name']) > -1 || this.state.selectedOptions.indexOf("all") > -1}
                                           onChange={this.handleOptionChange} />
                                    <OverlayTrigger delayShow={Constants.TOOLTIP_DELAY} placement="right"
                                                    overlay={<Popover id={`${layerName}-popover`}
                                                        title={layerName + ' - Layer Details'}>
                                                        <strong>Asset
                                                            Details: </strong>
                                                        <ul>
                                                            {layer.assets.map((asset) =>
                                                                <li>{asset}</li>)}
                                                        </ul>
                                                        <strong>Relationship
                                                            Details: </strong>
                                                        <ul>
                                                            {layer.relationships.map((relationship) =>
                                                                <li>{relationship}</li>)}
                                                        </ul>
                                                    </Popover>}>
                                        <span>{layerName + ' Layer'}</span>
                                    </OverlayTrigger>
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

    handleOptionChange(event) {
        var selected = event.target.value;
        var index = this.state.selectedOptions.indexOf(selected);
        if(index > -1){
            if(selected === "all"){
                this.setState({
                    ...this.state,
                    selectedOptions: []
                })
            } else {
                var selection = this.state.selectedOptions;
                selection.splice(index, 1);
                this.setState({
                    ...this.state,
                    selectedOptions: selection
                });
            }
        } else {
            this.setState({
                ...this.state,
                selectedOptions: [...this.state.selectedOptions, selected]
            })
        }
    }

    handleSubmit(){
        var selection = this.state.selectedOptions;
        this.props.dispatch(updateLayerSelection(selection));
    }

}

MultiViewLayerOption.propTypes = {
    modelId: PropTypes.string,
    layers: PropTypes.array,
    selectedLayers: PropTypes.array,
    dispatch: PropTypes.func,
};

export default MultiViewLayerOption;
