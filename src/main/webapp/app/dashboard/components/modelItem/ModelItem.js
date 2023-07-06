import React, { Component, Fragment } from "react";
import { Button, ButtonToolbar, Col, DropdownButton, MenuItem, Modal, OverlayTrigger, Panel, Popover, Row, Tooltip } from "react-bootstrap";
import { saveDownload } from "../../../common/actions/api";
import * as Constants from "../../../common/constants.js";
import { axiosInstance, axiosInstanceDashboard } from "../../../common/rest/rest";
import { deleteModel, updateModel } from "../../actions/api";
import DeleteModelModal from "../popups/DeleteModelModal";
import EditModelModal from "../popups/EditModelModal";
import ShareModelModal from "../popups/ShareModelModal";
import './ModelItem.scss';
import { callCopyModel } from "./ModelItemFunctions";

class ModelItem extends Component {

    constructor(props) {
        super(props);
        this.state = {
            open: false,
            editDetailsModal: false,
            shareDetailsModal: false,
            deleteModelModal: false,
            openLockedModal:false
        }

        this.formatRiskCalcMode = this.formatRiskCalcMode.bind(this);
        this.updateModel = this.updateModel.bind(this);
        this.deleteModel = this.deleteModel.bind(this);
        this.takeControl = this.takeControl.bind(this);
        this.renderVersionWarning = this.renderVersionWarning.bind(this);
    }

    renderVersionWarning(validatedDomainVersion, domainVersion) {
        let versionWarningText = "Version does not match current knowledgebase version (" + domainVersion + "). Please revalidate!";
        let versionMismatch = (validatedDomainVersion && (validatedDomainVersion !== domainVersion));
        return <OverlayTrigger delayShow={Constants.TOOLTIP_DELAY} placement="bottom"
                overlay={<Tooltip id="version-tooltip">
                    <strong>{versionWarningText}</strong></Tooltip>}>
                <div style={{
                    display: versionMismatch ? "inline-block" : "none",
                    marginLeft: "10px"
                }}>
                    <i className="fa fa-exclamation-triangle warning"></i>
                </div>
            </OverlayTrigger>
    }

    render() {
        let ontologies = this.getOntologies(this.props.ontologies);
        let {model, models} = this.props;
        let domainGraph = model.domainGraph;
        let domainVersion = model.domainVersion;
        let validatedDomainVersion = model.validatedDomainVersion;
        let temp = new Date(model.modified);
        let temp2 = new Date(model.created);
        let mod = new Date(model.modified);
        let creat = new Date(model.created);
        let temp3 = new Date().setHours(0, 0, 0, 0);
        let modDate = (temp.setHours(0,0,0,0) === temp3) ? "Today " + mod.toLocaleTimeString() : mod.toLocaleDateString();
        let created = (temp2.setHours(0,0,0,0) === temp3) ? "today " + creat.toLocaleTimeString() : creat.toLocaleDateString();

        let domain;
        for (let i = 0; i < ontologies.length; i++) {
            if(domainGraph === ontologies[i].graph){
                domain = ontologies[i].name;
                break;
            }
        }

        if (!domain) {
            console.warn("WARNING: domain model not installed: " + domainGraph);
            domain = domainGraph.replace("http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain-", "").toUpperCase();
            //TODO: display a warning for the user in this case
        }

        let statColor = "";
        let status = "";

        if (model.riskLevelsValid){
            statColor = "green";
            status = "Risk-calculated (" + this.formatRiskCalcMode(model.riskCalculationMode) + ")";
        } else if (model.valid && !model.riskLevelsValid) {
            statColor = "orange";
            status = "Validated";
        } else {
            statColor = "red";
            status = "Not Validated"
        }

        let statusStyle = {
            color: statColor
        }

        return (
            <div>
                <Modal show={this.state.openLockedModal}>
                    <Modal.Header closeButton>
                        <Modal.Title>Locked Model</Modal.Title>
                    </Modal.Header>
                    <Modal.Body>
                        <p>The model is being edited by another user.</p>
                    </Modal.Body>
                    <Modal.Footer>
                        <Button
                            autoFocus
                            onClick={() => {
                                this.setState({
                                    openLockedModal: false
                                });
                            }}
                        >
                            Cancel
                        </Button>
                        <Button
                            bsStyle="primary"
                            onClick={() => {
                                this.setState({
                                    openLockedModal: false
                                });
                                window.open(process.env.config.API_END_POINT + "/models/" + this.props.model.id + "/read");
                            }}
                        >
                            <span className="fa fa-eye" /> View model
                        </Button>
                        <Button
                            bsStyle="primary"
                            onClick={() => {
                                this.setState({
                                    openLockedModal: false
                                });
                                this.takeControl()
                            }}
                        >
                            <span className="fa fa-pencil" /> Edit model (take control)
                        </Button>
                    </Modal.Footer>
                </Modal>
                <Panel className="model-item-panel" onClick = {e => this.clickPanelAction(e)}>
                    <Row className="model-item-row">
                        <Col xs={2} md={2} >
                            {
                                this.state.open ?
                                    <Fragment>
                                        <span className="model-item-open">{model.name}</span>
                                        <ul className="model-detail">
                                            <li>Created: {created}</li>
                                            <li>Created by: {model.userId}</li>
                                            <li>Status: <span style={statusStyle}>{status}</span></li>
                                        </ul>
                                    </Fragment>
                                :
                                    <OverlayTrigger
                                        delayShow={Constants.TOOLTIP_DELAY}
                                        placement="right"
                                        overlay={
                                            <Popover
                                                id="model-title-popover"
                                                className={"tooltip-overlay"}
                                            >
                                                <span>{model.name}</span>
                                            </Popover>
                                        }
                                    >
                                        <span className="model-item-closed">
                                            {model.name}
                                        </span>
                                    </OverlayTrigger>
                            }
                        </Col>
                        <Col xs={3} md={3} >
                            {
                                this.state.open ?
                                    <span className="model-item-open">{model.description}</span>
                                :
                                    <span className="model-item-closed">{model.description}</span>
                            }
                        </Col>
                        <Col className="owner" xs={1} md={1} >
                            {
                                this.props.user.username == model.userId ?
                                    <span className="model-item-closed">Me</span>
                                :
                                    <span className="model-item-closed">{model.userId}</span>
                            }
                        </Col>
                        <Col xs={1} md={1} >
                            <span className="model-item-closed">{modDate}</span>
                        </Col>
                        <Col xs={1} md={1} >
                            {
                                this.state.open ?
                                    <span className="model-item-open">{domain}</span>
                                :
                                    <span className="model-item-closed">{domain}</span>
                            }
                        </Col>
                        <Col className="version" xs={2} md={2} >
                            {
                                this.state.open ?
                                    <span className="model-item-open">{validatedDomainVersion}
                                        {this.renderVersionWarning(validatedDomainVersion, domainVersion)}
                                    </span>
                                :
                                    <span className="model-item-closed">{validatedDomainVersion}
                                        {this.renderVersionWarning(validatedDomainVersion, domainVersion)}
                                    </span>
                            }
                        </Col>
                        <Col className="button-toolbar" xs={2} md={2} onClick={e => e.stopPropagation()}>
                            <ButtonToolbar className="model-item-btn-group">
                                {
                                    model.canBeEdited ?
                                        <Button className="icon-button icon-button-hidden" onClick={e => this.clickEditAction(e)}>
                                            <OverlayTrigger
                                                placement="top"
                                                delay={Constants.TOOLTIP_DELAY}
                                                overlay={
                                                    <Tooltip id="tooltip-edit">Edit model</Tooltip>
                                                }
                                            >
                                                <span className="fa fa-pencil" />
                                            </OverlayTrigger>
                                        </Button>
                                    :
                                        null
                                }
                                {
                                    model.canBeShared ?
                                        <Button className="icon-button icon-button-hidden" onClick={e => this.shareModel(e)}>
                                            <OverlayTrigger
                                                placement="top"
                                                delay={Constants.TOOLTIP_DELAY}
                                                overlay={
                                                    <Tooltip id="tooltip-share">Share model</Tooltip>
                                                }
                                            >
                                                <span className="fa fa-share" />
                                            </OverlayTrigger>
                                        </Button>
                                    :
                                        null
                                }
                                <Button className="icon-button icon-button-hidden" onClick={e => this.clickViewAction(e)}>
                                    <OverlayTrigger
                                        placement="top"
                                        delay={Constants.TOOLTIP_DELAY}
                                        overlay={
                                            <Tooltip id="tooltip-view">View model</Tooltip>
                                        }
                                    >
                                        <span className="fa fa-eye" />
                                    </OverlayTrigger>
                                </Button>
                                {(!this.state.open) ?
                                    <Button className="icon-button icon-button-hidden" onClick={e => this.open(e)}>
                                        <OverlayTrigger
                                            placement="top"
                                            delay={Constants.TOOLTIP_DELAY}
                                            overlay={
                                                <Tooltip id="tooltip-view">Expand for more detail</Tooltip>
                                            }
                                        >
                                            <span className="fa fa-chevron-down"/>
                                        </OverlayTrigger>
                                    </Button>
                                :
                                    <Button className="icon-button icon-button-hidden" onClick={e => this.close(e)}>
                                        <OverlayTrigger
                                            placement="top"
                                            delay={Constants.TOOLTIP_DELAY}
                                            overlay={
                                                <Tooltip id="tooltip-view">Close extra detail</Tooltip>
                                            }
                                        >
                                            <span className="fa fa-chevron-up"/>
                                        </OverlayTrigger>
                                    </Button>
                                }
                                <DropdownButton
                                    className="icon-button"
                                    title={<span className="fa fa-ellipsis-v" />}
                                    id="dropdown-size-medium"
                                    noCaret
                                    onClick={e => e.stopPropagation()}
                                >
                                    {
                                        model.canBeEdited ?
                                            <MenuItem onClick={e => this.clickEditAction(e, true)}>
                                                <div className="card-dropdown">
                                                    <span className="fa fa-pencil "></span>
                                                    <span>Edit model in new tab</span>
                                                </div>
                                            </MenuItem>
                                        :
                                            null
                                    }
                                    <MenuItem onClick={e => this.clickViewAction(e, true)}>
                                        <div className="card-dropdown">
                                            <span className="fa fa-eye "></span>
                                            <span>View model in new tab</span>
                                        </div>
                                    </MenuItem>
                                    {
                                        model.canBeEdited ?
                                            <MenuItem onClick={e => this.editDetails(e)}>
                                                <div className="card-dropdown">
                                                    <span className="fa fa-pencil-square-o "></span>
                                                    <span>Edit details</span>
                                                </div>
                                            </MenuItem>
                                        :
                                            null
                                    }
                                    <MenuItem onClick={e => {
                                        e.stopPropagation();
                                        this.props.dispatch(saveDownload("./models/" + model["id"] + "/export"));
                                    }}>
                                        <div className="card-dropdown">
                                            <span className="fa fa-download"></span>
                                            <span>Export full model</span>
                                        </div>
                                    </MenuItem>
                                    <MenuItem onClick={e => {
                                        e.stopPropagation();
                                        this.props.dispatch(saveDownload("./models/" + model["id"] + "/exportAsserted"));
                                    }}>
                                        <div className="card-dropdown">
                                            <span className="fa fa-download"></span>
                                            <span>Export assets only</span>
                                        </div>
                                    </MenuItem>
                                    <MenuItem onClick={e => callCopyModel(e, model, models, this.props.dispatch)}>
                                        <div className="card-dropdown">
                                            <span className="fa fa-copy"></span>
                                            <span>Copy model</span>
                                        </div>
                                    </MenuItem>
                                    {
                                        model.canBeShared ?
                                            <MenuItem onClick={e => this.deleteModelModal(e)}>
                                                <div className="card-dropdown">
                                                    <span className="fa fa-trash"></span>
                                                    <span>Delete</span>
                                                </div>
                                            </MenuItem>
                                        :
                                            null
                                    }
                                </DropdownButton>
                            </ButtonToolbar>
                        </Col>
                    </Row>
                </Panel>
                <EditModelModal show={this.state.editDetailsModal} model={model} updateModel={this.updateModel}
                                onHide={() => this.setState({...this.state, editDetailsModal: false})}/>

                {this.state.shareDetailsModal ? <ShareModelModal show={this.state.shareDetailsModal} model={model} dispatch={this.props.dispatch}
                    onHide={() => this.setState({ ...this.state, shareDetailsModal: false })} /> : <div></div>}

                <DeleteModelModal show={this.state.deleteModelModal} model={model} deleteModel={this.deleteModel}
                                  onHide={() => this.setState({...this.state, deleteModelModal: false})}/>
            </div>

        );
    }

    formatRiskCalcMode(mode) {
        //Capitalise first char, lower case the rest
        return mode.charAt(0).toUpperCase() + mode.slice(1).toLowerCase();
    }

    clickPanelAction(e) {
        e.stopPropagation();
        if (this.props.model.canBeEdited) {
            this.openForEdit();
        } else {
            this.openForRead();
        }
    }

    takeControl() {
        axiosInstance.post("/models/" + this.props.model.id + "/checkout")
            .then(() => {
                window.open(process.env.config.API_END_POINT + "/models/" + this.props.model.id + "/edit");
            }
        )
    }

    clickEditAction(e, newTab=false) {
        e.stopPropagation();
        this.openForEdit(newTab);
    }

    clickViewAction(e, newTab=false) {
        e.stopPropagation();
        this.openForRead(newTab);
    }

    openForEdit(newTab) {
        // Using different axios instance in order to manually catch errors
        // and not go through the global error handler as it causes issue discussed in #1148
        axiosInstanceDashboard.head("/models/" + this.props.model.id + "/edit")
            .then(() => {
                if (!this.state.openLockedModal) {
                    let url = process.env.config.API_END_POINT + "/models/" + this.props.model.id + "/edit";
                    if (newTab) {
                        window.open(url);
                    } else {
                        window.location.replace(url);
                    }
                }
            })
            .catch((error) => {
                if (error.response) {
                    let status = error.response.status;
                    if (status == '423') {
                        this.setState({ openLockedModal: true });
                    }
                }
            }
        )
    }

    openForRead(newTab) {
        let url = process.env.config.API_END_POINT + "/models/" + this.props.model.id + "/read";
        if (newTab) {
            window.open(url);
        } else {
            window.location.replace(url);
        }
    }

    open(e) {
        e.stopPropagation();
        this.setState({
            open: true
        })
    }

    close(e) {
        e.stopPropagation();
        this.setState({
            open: false
        })
    }

    getOntologies(ontologies) {
        if (jQuery.isEmptyObject(ontologies)) {
            console.warn("WARNING: no knowledgebases are currently available");
            return [];
        }

        return ontologies;
    }

    updateModel(id, updatedModel) {
        this.setState({ ...this.state, editDetailsModal: false });
        this.props.dispatch(updateModel(id, updatedModel));
    }

    deleteModel(id) {
        this.setState({...this.state, deleteModelModal: false});
        this.props.dispatch(deleteModel(id));
    }

    editDetails(e){
        e.stopPropagation();
        this.setState({editDetailsModal: !this.state.editDetailsModal});
    }

    shareModel(e){
        e.stopPropagation();
        this.setState({shareDetailsModal: !this.state.shareDetailsModal});
    }

    deleteModelModal(e){
        e.stopPropagation();
        this.setState({deleteModelModal: !this.state.deleteModelModal});
    }
}

export default ModelItem;
