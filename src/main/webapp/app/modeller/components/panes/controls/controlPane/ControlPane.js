import React from "react";
import PropTypes from "prop-types";
import {
    Button,
    SplitButton,
    MenuItem,
    OverlayTrigger,
    Tooltip,
    ButtonToolbar,
} from "react-bootstrap";
import * as Constants from "../../../../../common/constants.js";
import OptionsModal from "../options/OptionsModal";
import {
    getShortestPathPlot,
    getRecommendations,
    getValidatedModel,
    calculateRisks,
    calculateRisksBlocking,
    dropInferredGraph,
    modellerSetZoom,
} from "../../../../actions/ModellerActions";
import {
    getZoom,
    setZoom,
} from "../../../util/TileFactory";
import * as Input from "../../../../../common/input/input.js";

class ControlPane extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            optionsModal: {
                show: false,
            },
            //transformOrigin: [0.5, 0.5]
        };
        this.handleEdit = this.handleEdit.bind(this);

        this.handleZoomOutClick = this.handleZoomOutClick.bind(this);
        this.handleZoomInClick = this.handleZoomInClick.bind(this);
        this.handleSetZoom = this.handleSetZoom.bind(this);

        this.openOptionsModal = this.openOptionsModal.bind(this);
        this.closeOptionsModal = this.closeOptionsModal.bind(this);
    }

    /*
    componentWillMount() {
        this.setState({
            optionsModal: {
                show: false
            }
            //transformOrigin: [0.5, 0.5]
        })
    }
    */
    handleSetZoom(value) {
        let tempZoom = (1 / value) * 100;

        if (value < 0.01) {
            return;
        }

        setZoom(tempZoom);
        this.props.dispatch(modellerSetZoom(tempZoom, null));
    }
    handleZoomOutClick() {
        let zoomDelta = 0.05;
        let tempZoom = 1 / getZoom() - zoomDelta;
        tempZoom = 1 / tempZoom;
        if (tempZoom < 0.01) {
            return;
        }

        setZoom(tempZoom);
        this.props.dispatch(modellerSetZoom(tempZoom, null));
    }

    handleZoomInClick() {
        let zoomDelta = 0.05;
        let tempZoom = 1 / getZoom() + zoomDelta;
        tempZoom = Math.round((tempZoom + Number.EPSILON) * 100) / 100;

        tempZoom = 1 / tempZoom;

        if (tempZoom > 3) {
            return;
        }

        setZoom(tempZoom);
        this.props.dispatch(modellerSetZoom(tempZoom, null));
    }

    async handleEdit(event) {
        const self = this;
        const label = $(event.target);
        console.log(label);
        const textBox = label.next();
        label.hide();
        textBox.show();
        textBox.val(Input.unescapeString(label.html().replace("%", "")));

        textBox.mouseup(() => false);
        textBox.focus();
        textBox.select();

        textBox.on("blur", () => {
            let zoom = (1 / getZoom()) * 100;
            let newZoom = textBox.val();

            if (
                zoom !== newZoom &&
                parseInt(newZoom) > 1 &&
                !isNaN(parseInt(newZoom)) &&
                parseInt(newZoom) < 999 &&
                newZoom !== ""
            ) {
                this.handleSetZoom(parseInt(newZoom));
            }
            label.show();

            $(document).off("click");

            textBox.hide();
        });
        textBox.on("keyup", (e) => {
            if (e.keyCode === 13) {
                textBox.blur();
            }
        });
        $(document).on("click", (e) => {
            if (e.target.id !== textBox.attr("id") && textBox.is(":focus")) {
                textBox.blur();
            }
        });
    }
    /**
     * This renders our detail panel with any lists wanted.
     * @returns {XML} Returns the HTML that will be rendered to the Virtual DOM.
     */
    render() {
        let valid = this.props.model["valid"];
        let nAssets = this.props.model["assets"].length;

        let riskLevelsValid = valid && this.props.model["riskLevelsValid"]; //if model is invalid, riskLevelsValid must also be false
        let saved = this.props.model["saved"];
        let riskModeCurrent = false;
        let acceptableRiskLevel = Constants.ACCEPTABLE_RISK_LEVEL;

        return (
            <div
                ref="control-panel"
                classID="controlPanel"
                className="controls"
            >
                <div className="zoom">
                    <label
                        className="zoom-level-label"
                        type="text"
                        maxLength="4"
                        onClick={this.handleEdit}
                    >
                        {Math.ceil((1 / getZoom()) * 100)}%
                    </label>
                    <input id='set-zoom' type='text' class='text-primary' maxLength="4"/>
                    <div className="zoom-buttons">
                        <OverlayTrigger
                            delayShow={Constants.TOOLTIP_DELAY}
                            placement="bottom"
                            trigger={["hover"]}
                            rootClose
                            overlay={
                                <Tooltip id="zoom-out-tooltip">
                                    Zoom Out
                                </Tooltip>
                            }
                        >
                            <Button
                                bsStyle="info"
                                bsSize="xsmall"
                                disabled={
                                    this.props.loading.model ||
                                    this.props.model.validating ||
                                    this.props.model.calculatingRisks
                                }
                                onClick={this.handleZoomOutClick}
                            >
                                <i className="fa fa-search-minus" />
                            </Button>
                        </OverlayTrigger>
                        <OverlayTrigger
                            delayShow={Constants.TOOLTIP_DELAY}
                            placement="bottom"
                            trigger={["hover"]}
                            rootClose
                            overlay={
                                <Tooltip id="zoom-in-tooltip">Zoom In</Tooltip>
                            }
                        >
                            <Button
                                bsStyle="info"
                                bsSize="xsmall"
                                disabled={
                                    this.props.loading.model ||
                                    this.props.model.validating ||
                                    this.props.model.calculatingRisks
                                }
                                onClick={this.handleZoomInClick}
                            >
                                <i className="fa fa-search-plus" />
                            </Button>
                        </OverlayTrigger>
                    </div>
                </div>
                <ButtonToolbar>
                <OverlayTrigger
                    delayShow={Constants.TOOLTIP_DELAY}
                    placement="bottom"
                    trigger={["hover"]}
                    rootClose
                    overlay={
                        <Tooltip id="validate-tooltip">
                            Validate Model and Find Threats
                        </Tooltip>
                    }
                >
                    {!this.props.developerMode ? <Button
                        bsStyle={
                            valid === true
                                ? "success"
                                : valid === false
                                ? "danger"
                                : "info"
                        }
                        disabled={
                            this.props.loading.model ||
                            this.props.model.validating ||
                            this.props.model.calculatingRisks ||
                            nAssets == 0 ||
                            !this.props.authz.userEdit
                        }
                        onClick={() => {
                            this.props.dispatch(
                                getValidatedModel(this.props.model["id"])
                            );
                        }}
                    >
                        <i className="fa fa-play" />
                    </Button> :
                    <SplitButton 
                            bsStyle={
                                valid === true
                                    ? "success"
                                    : valid === false
                                    ? "danger"
                                    : "info"
                            }
                            title={<i className="fa fa-play"/>}
                            dropup={this.props.dropup || false}
                            pullRight={this.props.pullRight || false}
                            disabled={
                                this.props.loading.model ||
                                this.props.model.validating ||
                                this.props.model.calculatingRisks ||
                                nAssets == 0 ||
                                !this.props.authz.userEdit
                            }                                
                            onClick={() => {
                                this.props.dispatch(getValidatedModel(this.props.model["id"]));
                            }}>
                        <MenuItem eventKey={1}
                                  onClick={() => {
                                      this.props.dispatch(dropInferredGraph(this.props.model["id"]));
                                  }}>
                            Drop inferred graph
                        </MenuItem>
                    </SplitButton> }
                </OverlayTrigger>

                <OverlayTrigger
                    delayShow={Constants.TOOLTIP_DELAY}
                    placement="bottom"
                    trigger={["hover"]}
                    rootClose
                    overlay={
                        <Tooltip id="risk-calc-tooltip">
                            Calculate Risks{" "}
                            {riskLevelsValid
                                ? saved ? "(currently valid, saved)" : "(currently valid but unsaved)"
                                : "(currently invalid)"}
                        </Tooltip>
                    }
                >
                    <SplitButton
                        bsStyle={
                            riskLevelsValid === true
                                ? saved === true ? "success" : "warning"
                                : riskLevelsValid === false
                                    ? "danger"
                                    : "info"
                        }
                        title={<i className="fa fa-play-circle-o"/>}
                        disabled={
                            this.props.loading.model ||
                            this.props.model.validating ||
                            this.props.model.calculatingRisks ||
                            !valid ||
                            nAssets == 0 ||
                            !this.props.authz.userEdit
                        }
                        onClick={() => {
                            this.props.dispatch(
                                calculateRisksBlocking(this.props.model["id"], "FUTURE", true) // save
                            );
                        }}
                    >
                        <MenuItem eventKey={1}
                                  onClick={() => {
                                      this.props.dispatch(calculateRisksBlocking(this.props.model["id"], "FUTURE", true)); // save
                                  }}>
                            <strong>Calculate future risk (saved)</strong>
                        </MenuItem>
                        <MenuItem eventKey={2}
                                  onClick={() => {
                                      this.props.dispatch(calculateRisksBlocking(this.props.model["id"], "FUTURE", false)); // don't save
                                  }}>
                            Calculate future risk (unsaved)
                        </MenuItem>
                        <MenuItem eventKey={3}
                                  onClick={() => {
                                      this.props.dispatch(calculateRisksBlocking(this.props.model["id"], "CURRENT", true)); // save
                                  }}>
                            Calculate current risk (saved)
                        </MenuItem>
                        <MenuItem eventKey={4}
                                  onClick={() => {
                                      this.props.dispatch(calculateRisksBlocking(this.props.model["id"], "CURRENT", false)); // don't save
                                  }}>
                            Calculate current risk (unsaved)
                        </MenuItem>
                    </SplitButton>
                </OverlayTrigger>

                <OverlayTrigger
                    delayShow={Constants.TOOLTIP_DELAY}
                    placement="bottom"
                    trigger={["hover"]}
                    rootClose
                    overlay={
                        <Tooltip id="shortest-path-graph-options-tooltip">
                            Show attack path {" "}
                            {riskModeCurrent ? "(current risk)": "(future risk)" }
                        </Tooltip>
                    }
                >
                    <SplitButton
                        bsStyle={
                            riskLevelsValid === true
                                ? saved === true ? "success" : "warning"
                                : riskLevelsValid === false
                                    ? "danger"
                                    : "info"
                        }
                        title={<i className="fa fa-sitemap"/>}
                        disabled={
                            this.props.loading.model ||
                            this.props.model.validating ||
                            this.props.model.calculatingRisks ||
                            !valid ||
                            nAssets == 0 ||
                            !this.props.authz.userEdit
                        }
                        onClick={() => {
                            this.props.dispatch(
                                getShortestPathPlot(this.props.model["id"], "FUTURE") // don't save
                            );
                        }}
                    >
                        <MenuItem eventKey={1}
                                  onClick={() => {
                                      this.props.dispatch(getShortestPathPlot(this.props.model["id"], "FUTURE"));
                                  }}>
                            <strong>Show attack path (future risk)</strong>
                        </MenuItem>
                        <MenuItem eventKey={2}
                                  onClick={() => {
                                      this.props.dispatch(getShortestPathPlot(this.props.model["id"], "CURRENT"));
                                  }}>
                            Show attack path (current risk)
                        </MenuItem>
                        <MenuItem eventKey={3}
                                  onClick={() => {
                                      this.props.dispatch(getRecommendations(this.props.model["id"], "FUTURE", acceptableRiskLevel));
                                  }}>
                            Recommendations (future risk)
                        </MenuItem>
                        <MenuItem eventKey={4}
                                  onClick={() => {
                                      this.props.dispatch(getRecommendations(this.props.model["id"], "CURRENT", acceptableRiskLevel));
                                  }}>
                            Recommendations (current risk)
                        </MenuItem>
                    </SplitButton>
                </OverlayTrigger>
                </ButtonToolbar>
                <OptionsModal
                    dispatch={this.props.dispatch}
                    model={this.props.model}
                    layers={this.props.layers}
                    selectedLayers={this.props.selectedLayers}
                    show={this.state.optionsModal.show}
                    onHide={this.closeOptionsModal}
                />

            </div>
        );
    }

    openOptionsModal() {
        this.setState({
            ...this.state,
            optionsModal: {
                show: true,
            },
        });
    }

    closeOptionsModal() {
        this.setState({
            ...this.state,
            optionsModal: {
                show: false,
            },
        });
    }
}

ControlPane.propTypes = {
    model: PropTypes.object,
    canvas: PropTypes.object,
    layers: PropTypes.array,
    selectedLayers: PropTypes.array,
    //handleZoomInClick: PropTypes.func,
    //handleZoomOutClick: PropTypes.func,
    dispatch: PropTypes.func,
    loading: PropTypes.object,
    authz: PropTypes.object,
    //isValidating: PropTypes.bool,
    developerMode: PropTypes.bool
};

/* Export the detail panel as required. */
export default ControlPane;
