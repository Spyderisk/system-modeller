import React from "react";
import PropTypes from "prop-types";
import {Modal, Button, ProgressBar} from "react-bootstrap";
import {
    pollForLoadingProgress, pollForValidationProgress, pollForRiskCalcProgress, pollForRecommendationsProgress,
    validationCompleted, validationFailed,
    riskCalcCompleted, riskCalcFailed, changeSelectedAsset
    //resetValidation
} from "../../actions/ModellerActions";

class LoadingOverlay extends React.Component {

    constructor(props) {
        super(props);

        this.state = {
            showModal: false,
            stage: "",
            timeout: 0,
            increment: 1000,
            bounds: {
                loading: {
                    min: 1000,
                    max: 10000
                },
                validation: {
                    min: 5000,
                    max: 30000
                },
                riskcalc: {
                    min: 1000,
                    max: 2000
                }
            },
            progress: 0,
        };

        this.checkProgress = this.checkProgress.bind(this);
        this.pollValidationProgress = this.pollValidationProgress.bind(this);
        this.pollRiskCalcProgress = this.pollRiskCalcProgress.bind(this);
        this.pollRecommendationsProgress = this.pollRecommendationsProgress.bind(this);
        this.pollLoadingProgress = this.pollLoadingProgress.bind(this);
        this.pollDroppingInfGraphProgress = this.pollDroppingInfGraphProgress.bind(this);
        this.getValidationTimeout = this.getValidationTimeout.bind(this);
        this.getRiskCalcTimeout = this.getRiskCalcTimeout.bind(this);
        this.getRecommendationsTimeout = this.getRecommendationsTimeout.bind(this);
        this.getLoadingTimeout = this.getLoadingTimeout.bind(this);
        this.getTimeout = this.getTimeout.bind(this);
        this.getHeaderText = this.getHeaderText.bind(this);

    }

    componentDidMount() {
        //console.log("LoadingOverlay timeout settings: ", this.state);
    }

    componentWillReceiveProps(nextProps) {
        let showModal = this.state.showModal;
        let timeout = this.state.timeout;
        let progress = this.state.progress;
        let stage = this.state.stage;
        let stateChanged = false;

        // If validation has completed, show modal (or at least set showModal flag; modal actually gets displayed once loading has finished)
        if (this.props.isValidating && !nextProps.isValidating) {
            //console.log("LoadingOverlay: setting showModal true");
            stage = "Validation";
            showModal = true;
            stateChanged = true;
        }
        
        // If risk calc has completed, show modal
        if (this.props.isCalculatingRisks && !nextProps.isCalculatingRisks) {
            stage = "Risk calculation";
            if (nextProps.validationProgress.status !== "inactive") showModal = true;
            stateChanged = true;
        }
        
        // If risk calc has completed, show modal
        if (this.props.isCalculatingRecommendations && !nextProps.isCalculatingRecommendations) {
            stage = "Recommendations";
            if (nextProps.validationProgress.status !== "inactive") showModal = true;
            stateChanged = true;
        }

        // Show modal (error dialog) if loading has failed
        if (this.props.loadingProgress.status !== "failed" && nextProps.loadingProgress.status === "failed") {
            showModal = true;
            stateChanged = true;
        }

        if (!this.props.isDroppingInferredGraph && nextProps.isDroppingInferredGraph) {
            //console.log("Dropping inferred graph - initialising timeout");
            stage = "DroppingInferredGraph";
            timeout = 0;
            stateChanged = true;
        }        

        if (this.props.isDroppingInferredGraph && !nextProps.isDroppingInferredGraph) {
            stage = "DroppingInferredGraph";
            progress = 1.0;
            stateChanged = true;
        }        

        // If loading has started, start polling
        if (!this.props.isLoading && nextProps.isLoading) {
            stage = "Loading";
            timeout = 0;
            stateChanged = true;
        }
        // If validation has started, start polling
        else if (!this.props.isValidating && nextProps.isValidating) {
            stage = "Validation";
            timeout = 0;
            stateChanged = true;
        }
        // If risk calc has started, start polling
        else if (!this.props.isCalculatingRisks && nextProps.isCalculatingRisks) {
            stage = "Risk calculation";
            timeout = 0;
            stateChanged = true;
        }
        // If recommendations has started, start polling
        else if (!this.props.isCalculatingRecommendations && nextProps.isCalculatingRecommendations) {
            stage = "Recommendations";
            timeout = 0;
            stateChanged = true;
        }

        if (stateChanged) {
            //here, we set both changes of state at the same time, otherwise initial change may be ignored
            this.setState({...this.state,
                showModal: showModal,
                stage: stage,
                timeout: timeout,
                progress: progress
            });
        }

    }

    componentDidUpdate(prevProps, prevState) {
        // If loading has started, start polling
        if (!prevProps.isLoading && this.props.isLoading) {
            if (this.props.loadingId) {
                console.log("LoadingOverlay: loading started, loadingId received. Start polling");
                this.checkProgress();
            }
            else {
                console.log("LoadingOverlay: loading started. Waiting for loadingId..");
            }
        }
        else if (!prevProps.loadingId && this.props.loadingId) {
            console.log("LoadingOverlay: received loadingId: " + this.props.loadingId);
            this.checkProgress();
        }
        else if (!prevProps.isValidating && this.props.isValidating) {
            this.checkProgress();
        }
        else if (!prevProps.isCalculatingRisks && this.props.isCalculatingRisks) {
            this.checkProgress();
        }
        else if (!prevProps.isCalculatingRecommendations && this.props.isCalculatingRecommendations) {
            this.checkProgress();
        }
        else if (prevProps.loadingProgress.waitingForUpdate && !this.props.loadingProgress.waitingForUpdate) {
            this.checkProgress();
        }
        else if (prevProps.validationProgress.waitingForUpdate && !this.props.validationProgress.waitingForUpdate) {
            this.checkProgress();
        }
        else if (this.props.isDroppingInferredGraph) {
            this.checkProgress();
        }
    }

    getValidationTimeout() {
        let min = this.state.bounds.validation.min;
        let max = this.state.bounds.validation.max;
        let progress = this.props.validationProgress.progress;
        return this.getTimeout(min, max, progress);
    }

    getRiskCalcTimeout() {
        let min = this.state.bounds.riskcalc.min;
        let max = this.state.bounds.riskcalc.max;
        let progress = this.props.validationProgress.progress;
        return this.getTimeout(min, max, progress);
    }

    getRecommendationsTimeout() {
        return getRiskCalcTimeout();
    }

    getLoadingTimeout() {
        let min = this.state.bounds.loading.min;
        let max = this.state.bounds.loading.max;
        let progress = this.props.loadingProgress.progress;
        return this.getTimeout(min, max, progress);
    }

    getTimeout(min, max, progress) {
        let timeout = this.state.timeout;
        let increment = this.state.increment;

        // increment timeout initially, then decrement towards end
        if (progress < 0.4) {
            timeout += increment;
        }
        else if (progress > 0.8) {
            timeout -= increment;
        }

        // check within min/max bounds
        timeout = (timeout < min) ? min : (timeout > max) ? max : timeout;

        //update state
        this.setState({...this.state,
            timeout: timeout});

        return timeout;
    }

    checkProgress() {
        if (this.props.isValidating) {
            if (this.props.validationProgress.progress >= 1.0) {
                if (this.props.validationProgress.status === "completed") {
                    console.log("LoadingOverlay: validation progress completed");
                    this.props.dispatch(validationCompleted(this.props.modelId));
                }
                else if (this.props.validationProgress.status === "failed") {
                    console.log("LoadingOverlay: validation progress failed");
                    this.props.dispatch(validationFailed(this.props.modelId));
                }
            } else {
                setTimeout(this.pollValidationProgress, this.getValidationTimeout());
            }
        }
        else if (this.props.isCalculatingRisks) {
            if (this.props.validationProgress.status === "inactive") {
                console.log("WARNING: isCalculatingRisks is true, but status is inactive");
                this.props.dispatch(riskCalcFailed(this.props.modelId));
                return;
            }
            else if (this.props.validationProgress.progress >= 1.0) {
                if (this.props.validationProgress.status === "completed") {
                    console.log("LoadingOverlay: risk calc progress completed");
                    this.props.dispatch(riskCalcCompleted(this.props.modelId));
                }
                else if (this.props.validationProgress.status === "failed") {
                    console.log("LoadingOverlay: risk calc progress failed");
                    this.props.dispatch(riskCalcFailed(this.props.modelId));
                }
                else {
                    //This should not be necessary, but if server has not yet set completed state..
                    setTimeout(this.pollRiskCalcProgress, this.getRiskCalcTimeout());
                }
            } else {
                setTimeout(this.pollRiskCalcProgress, this.getRiskCalcTimeout());
            }
        }
        else if (this.props.isCalculatingRecommendations) {
            if (this.props.validationProgress.status === "inactive") {
                console.log("WARNING: isCalculatingRecommendations is true, but status is inactive");
                this.props.dispatch(recommendationsFailed(this.props.modelId));
                return;
            }
            else if (this.props.validationProgress.progress >= 1.0) {
                if (this.props.validationProgress.status === "completed") {
                    console.warn("LoadingOverlay: recommendations progress completed");
                }
                else if (this.props.validationProgress.status === "failed") {
                    console.warn("LoadingOverlay: recommendations progress failed");
                    this.props.dispatch(recommendationsFailed(this.props.modelId));
                }
                else {
                    //This should not be necessary, but if server has not yet set completed state..
                    setTimeout(this.pollRecommendationsProgress, this.getRecommendationsTimeout());
                }
            } else {
                setTimeout(this.pollRecommendationsProgress, this.getRecommendationsTimeout());
            }
        }
        else if (this.props.isLoading) {
            if (this.props.loadingProgress.progress >= 1.0) {
                //console.log("LoadingOverlay: loading progress completed");
            } else {
                let timeout = this.getLoadingTimeout();
                setTimeout(this.pollLoadingProgress, timeout);
            }
        }
        else if (this.props.isDroppingInferredGraph) {
            setTimeout(this.pollDroppingInfGraphProgress, 1000);
        }
    }

    pollValidationProgress() {
        // Unlike pollRiskCalcProgress (below), we don't need to check isValidating flag here
        // as only the progress response can determine the state of the validation
        this.props.dispatch(pollForValidationProgress(this.props.modelId));
    }

    pollRiskCalcProgress() {
        // While synchronous risk calc is running, the isCalculatingRisks flag is true, so poll for progress
        // Once the risk calc call returns, the flag is set to false, so we avoid an unnecessary progress request below
        if (this.props.isCalculatingRisks) {
            this.props.dispatch(pollForRiskCalcProgress(this.props.modelId));
        }
        else {
            console.log("Risk calculation complete. Cancel polling for progress");
        }
    }

    pollRecommendationsProgress() {
        // While synchronous recommendations is running, the isCalculatingRecommendations flag is true, so poll for progress
        // Once the recommendations call returns, the flag is set to false, so we avoid an unnecessary progress request below
        if (this.props.isCalculatingRecommendations) {
            this.props.dispatch(pollForRecommendationsProgress(this.props.modelId));
        }
        else {
            console.log("Recommendations complete. Cancel polling for progress");
        }
    }

    pollLoadingProgress() {
        if (this.props.loadingId) {
            this.props.dispatch(pollForLoadingProgress($("meta[name='_model']").attr("content"), this.props.loadingId));
        }
        else {
            console.warn("loadingId not yet available");
        }
    }

    pollDroppingInfGraphProgress() {
        let progress = this.state.progress;
        if (progress < 1.0) {
            let updatedProgress = Math.round((progress + 0.1) * 10) / 10;
            console.log("pollDroppingInfGraphProgress: ", updatedProgress);
            this.setState({...this.state, progress: updatedProgress})
        }
    }

    getHeaderText() {
        let header = "";

        if (this.props.isValidating) {
            header = "The model is currently validating";
        }
        else if (this.props.isCalculatingRisks) {
            header = "Calculating risks";
        }
        else if (this.props.isCalculatingRecommendations) {
            header = "Calculating recommendations";
        }

        return header;
    }

    render() {
        let isCalculatingRisks = this.props.isCalculatingRisks && this.props.validationProgress.status !== "inactive";
        let isCalculatingRecommendations = this.props.isCalculatingRecommendations && this.props.validationProgress.status !== "inactive";
        let isDroppingInferredGraph = this.props.isDroppingInferredGraph;
        let clazz = "loading-overlay " + (this.props.isValidating || isCalculatingRisks || isCalculatingRecommendations || isDroppingInferredGraph || this.props.isLoading ? "visible" : "invisible");
        let stage = this.state.stage;
        let headerText = this.getHeaderText();
        
        return (
            <div className={clazz}>
                {(this.props.isValidating || isCalculatingRisks || isCalculatingRecommendations) &&
                    <div>
                        <h1>{headerText}...</h1>
                        <span className="fa fa-cog fa-spin fa-4x fa-fw"/>
                        <div style={{width: "100%"}}>
                            <h2>{this.props.validationProgress.message}...</h2>
                            <ProgressBar active bsStyle={this.props.validationProgress.status === "failed" ? "danger" : "success"}
                                         label={`${Math.round(this.props.validationProgress.progress * 100)}%`}
                                         now={Math.round(this.props.validationProgress.progress * 100)}/>
                        </div>
                    </div>
                }
                
                {this.props.isLoading &&
                    <div>
                        <h1>The model is currently loading...</h1>
                        <span className="fa fa-refresh fa-spin fa-4x fa-fw"/>
                        <div style={{width: "100%"}}>
                            <h2>{this.props.loadingProgress.message}...</h2>
                            <ProgressBar active label={`${Math.round(this.props.loadingProgress.progress * 100)}%`}
                                         now={Math.round(this.props.loadingProgress.progress * 100)}/>
                        </div>
                    </div>
                }

                {isDroppingInferredGraph && 
                    <div>
                        <h1>Dropping inferred graph...</h1>
                        <span className="fa fa-cog fa-spin fa-4x fa-fw"/>
                        <div style={{width: "100%"}}>
                            <ProgressBar active label={`${Math.round(this.state.progress * 100)}%`}
                                         now={Math.round(this.state.progress * 100)}/>
                        </div>
                    </div>
                }

                <Modal show={this.state.showModal && !this.props.isLoading} onHide={() => this.setState({...this.state, showModal: false})}>
                    <Modal.Body>
                        <div className="validation-modal">
                            <div className="confirmation">
                                {
                                    this.props.isLoading ?
                                        <span className="fa fa-5x fa-cog" />
                                        :
                                        this.props.validationProgress.status !== "failed" ?
                                            this.props.isValid && (this.props.loadingProgress.status !== "failed") ?
                                                <span className="fa fa-5x fa-check" />
                                                :
                                                <span className="fa fa-5x fa-times" />
                                            :
                                            <span className="fa fa-5x fa-times" />
                                }
                                {
                                    this.props.isLoading ?
                                        <h3>Model is currently loading...</h3>
                                        :
                                        this.props.validationProgress.status !== "failed" ?
                                            this.props.loadingProgress.status === "failed" ?
                                                <h3>Loading failed</h3>
                                                :
                                                this.props.isValid ?
                                                    this.props.hasModellingErrors ?
                                                        <div>
                                                            <h3>Validation completed</h3>
                                                            <h4>Warning: unaddressed possible modelling errors exist</h4>
                                                        </div>
                                                        :
                                                        <h3>Model was validated successfully</h3>
                                                    :
                                                    <h3>Model has issues to fix</h3>
                                            :
                                            <h3>{stage} failed</h3>
                                }
                            </div>
                            <div className="details" style={{display: (this.props.validationProgress.error !== "" || this.props.loadingProgress.error !== "") ? "block" : "none"}}>
                                <div className="option-well well">
                                    {this.props.validationProgress.error !== "" ? <h5>{"ERROR: " + this.props.validationProgress.error}</h5>
                                        : <h5>{"ERROR: " + this.props.loadingProgress.error}</h5>}
                                </div>
                            </div>
                            <Button bsStyle={this.props.validationProgress.status === "failed" ? "danger" : "primary"}
                                    onClick={() => {
                                                        this.setState({...this.state, showModal: false});
                                                        if (this.props.hasModellingErrors) {
                                                            this.props.dispatch(changeSelectedAsset(""));
                                                        }
                                                   }
                                    }
                                    >Continue</Button>
                        </div>

                    </Modal.Body>
                </Modal>
            </div>
        )
    }
}

LoadingOverlay.propTypes = {
    modelId: PropTypes.string,
    loadingId: PropTypes.string,
    isValidating: PropTypes.bool,
    isCalculatingRisks: PropTypes.bool,
    isCalculatingRecommendations: PropTypes.bool,
    isValid: PropTypes.bool,
    hasModellingErrors: PropTypes.bool,
    validationProgress: PropTypes.object,
    isLoading: PropTypes.bool,
    isDroppingInferredGraph: PropTypes.bool,
    loadingProgress: PropTypes.object,
    dispatch: PropTypes.func
};

export default LoadingOverlay;
