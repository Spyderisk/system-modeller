import React from "react";
import PropTypes from 'prop-types';
import {Button} from "react-bootstrap";

class SlidersPanel extends React.Component {

    constructor(props) {
        super(props);
    }

    render() {
        var priorLikelihood = 0.0;
        var potentialImpact = 0.0;

        if (this.props.threat.threatLevel !== null) {
            priorLikelihood = this.props.threat.threatLevel["priorLikelihood"];
            potentialImpact = this.props.threat.threatLevel["potentialImpact"];
        }

        return (
            <div>
                <div className="threat-slider container-fluid">
                    <div className="row">
                        <span className="col-md-12"><strong>{"Prior Likelihood: "}</strong><span
                            classID="likelihoodSlider">{priorLikelihood}</span>
                        </span>
                    </div>
                    <div className="row">
                        <input classID="priorLikelihood"
                               type="range"
                               min="0"
                               max="1"
                               step="0.01"
                               defaultValue={priorLikelihood}
                               onChange={() => {
                                               $("span[classid='likelihoodSlider']").text($("input[classid='priorLikelihood']").val());
                                           }}/>
                    </div>
                </div>
                <div className="threat-slider container-fluid">
                    <div className="row">
                        <span className="col-md-12">
                            <strong>
                                {"Potential Impact Level: "}
                            </strong>
                            <span classID="impactSlider">
                                {potentialImpact}
                            </span>
                        </span>
                    </div>
                    <div className="row">
                        <input classID="potentialImpact"
                               type="range"
                               min="0"
                               max="1"
                               step="0.01"
                               defaultValue={potentialImpact}
                               onChange={() => {
                                               $("span[classid='impactSlider']").text($("input[classid='potentialImpact']").val());
                                           }}/>
                    </div>
                </div>
                <div className="threat-slider-save container-fluid">
                    <div className="row">
                        <div className="col-md-12">
                            <Button bsStyle="primary"
                                    bsSize="small"
                                    onClick={() => {
                                                    var priorLikelihood = $("input[classid='priorLikelihood']").val(),
                                                        potentialImpact = $("input[classid='potentialImpact']").val();
                                                    this.props.updateThreat({
                                                        threatLevel: {
                                                            priorLikelihood: priorLikelihood,
                                                            potentialImpact: potentialImpact
                                                        }
                                                    })
                                                }}>
                                {"Save Threat Level"}
                            </Button>
                        </div>
                    </div>
                </div>
            </div>
        );
    }

}

SlidersPanel.propTypes = {
    threat: PropTypes.object,
    updateThreat: PropTypes.func
};

export default SlidersPanel;
