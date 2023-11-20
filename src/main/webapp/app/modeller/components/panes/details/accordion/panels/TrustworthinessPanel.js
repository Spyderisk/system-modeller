import React from "react";
import PropTypes from 'prop-types';
import {Form, FormGroup, Checkbox} from 'react-bootstrap';
import {updateTwasOnAsset, getRootCauses, revertAssertedTwasOnAsset, toggleFilter} from "../../../../../actions/ModellerActions";

class TrustworthinessPanel extends React.Component {

    constructor(props) {
        super(props);

        this.getUpdatedState = this.getUpdatedState.bind(this);
        this.renderTrustworthiness = this.renderTrustworthiness.bind(this);
        this.twValueChanged = this.twValueChanged.bind(this);
        this.openMisbehaviourExplorer = this.openMisbehaviourExplorer.bind(this);

        this.state = this.getUpdatedState(props);;
    }
    
    getUpdatedState(props) {
        let asset = props.asset;

        if (asset === undefined)
            return;

        let levels = Object.values(props.levels).sort(function(a, b) {
            return b.value - a.value;
        });

        let twasArr = props.twas.sort(function (a, b) {
            return (a.attribute.label < b.attribute.label) ? -1 : (a.attribute.label > b.attribute.label) ? 1 : 0;
        });

        let attributes = [];
        let updating = {};
        let twas = {};

        twasArr.map((twa, index) => {
            if (twa.asset !== asset.uri) {
                console.log("WARNING: twa not for this asset (ignoring)");
                return;
            };

            let attribute = twa.attribute;
            let label = attribute.label;
            if (! twa.assertedTWLevel) {
                console.log("WARNING: null assertedTWLevel on asset: " + label);
            }
            else {
                attributes.push(attribute);
                twas[label] = twa;
                updating[label] = false;
            }
        });

        let updatedState = {
            levels: levels,
            attributes: attributes,
            updating: updating,
            twas: twas
        };
        
        return updatedState;
    }
       
    //N.B. this method is deprecated, so code will need to be refactored to use preferred methods!
    componentWillReceiveProps(nextProps) {
        let updatedState = this.getUpdatedState(nextProps);
        this.setState(updatedState);
    }
    
    render() {
        let asset = this.props.asset;

        return (
            <div className="trustworthiness detail-list">
                <div className="container-fluid">
                    { (asset !== undefined) && (this.state.attributes.length > 0)
                        ? this.renderTrustworthiness()
                        : <span>No data found</span>
                    }
                </div>
            </div>
        );
    }

    renderTrustworthiness() {
        let self = this;

        let attributes = this.state.attributes;
        if (attributes === undefined)
            attributes = [];

        let levels = this.state.levels;
        
        //flag to hide TWAS where visible = false
        let hideInvisibleTwas = this.props.filters.assetDetails.twas.hideInvisible;
        
        return (
            <div>
                <Form>
                    <FormGroup>
                        <Checkbox
                            checked={hideInvisibleTwas}
                            onChange={(e) => {
                                this.setFilter(e.nativeEvent.target.checked)
                            }}>
                            Only visible attributes
                        </Checkbox>
                    </FormGroup>
                </Form>
                {this.props.renderTrustworthinessAttributes(attributes, levels, self, hideInvisibleTwas)}
            </div>
        )
    }

    setFilter(value) {
        this.props.dispatch(toggleFilter("twas", "hideInvisible", value));
    }

    twValueChanged(e) {
        let field = e.target.id;
        let selectedLevelUri = e.target.value;

        let selectedLevel = this.props.levels.find((level) => level["uri"] === selectedLevelUri);

        let twas = this.state.twas;
        let updating = this.state.updating;

        //create new copy of twas object, with updated assertedTWLevel
        let twasForField = {...twas[field],
            assertedTWLevel: selectedLevel
        };

        let updatedTwas = {...twas};
        updatedTwas[field] = twasForField;

        //set updating flag for this field
        let updatedUpdating = {...updating};
        updatedUpdating[field] = true;

        this.setState({
            ...this.state,
            twas: updatedTwas,
            updating: updatedUpdating
        });

        this.props.dispatch(updateTwasOnAsset(this.props.modelId, this.props.asset.id, twasForField));
    }

    //Used via renderTrustworthinessAttributes method in Modeller.js
    onClickRevertTwasLevel(twas) {
        if (twas) {
            let twasLabel = twas.attribute.label;

            //set updating flag for this twas label
            let updatedUpdating = {...this.state.updating};
            updatedUpdating[twasLabel] = true;

            this.setState({
                ...this.state,
                updating: updatedUpdating
            });

            this.props.dispatch(revertAssertedTwasOnAsset(this.props.modelId,  this.props.asset.id, twas));
        }
    }

    openMisbehaviourExplorer(misbehaviour) {
        let m;
        if (misbehaviour.m !== undefined) {
            m = misbehaviour.m;
        }
        else {
            m = misbehaviour;
        }

       let updateRootCausesModel = true;
       this.props.dispatch(getRootCauses(this.props.modelId, m, updateRootCausesModel));
    }

}

TrustworthinessPanel.propTypes = {
    modelId: PropTypes.string,
    levels: PropTypes.array,
    asset: PropTypes.object,
    twas: PropTypes.array,
    filters: PropTypes.object,
    renderTrustworthinessAttributes: PropTypes.func,
    dispatch: PropTypes.func
};

export default TrustworthinessPanel;
