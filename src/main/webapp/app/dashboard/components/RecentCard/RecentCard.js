import React, { Component } from "react";
import { Button, ButtonToolbar, Col, DropdownButton, MenuItem, Modal, OverlayTrigger, Panel, Popover, Tooltip } from 'react-bootstrap';
import PanelBody from "react-bootstrap/lib/PanelBody";
import PanelFooter from "react-bootstrap/lib/PanelFooter";
import PanelHeading from "react-bootstrap/lib/PanelHeading";
import { saveDownload } from "../../../common/actions/api";
import * as Constants from "../../../common/constants.js";
import { axiosInstance, axiosInstanceDashboard } from "../../../common/rest/rest";
import { deleteModel, updateModel } from "../../actions/api";
import { callCopyModel } from "../modelItem/ModelItemFunctions";
import DeleteModelModal from "../popups/DeleteModelModal";
import EditModelModal from "../popups/EditModelModal";
import ShareModelModal from "../popups/ShareModelModal";
import './RecentCard.scss';

class RecentCard extends Component {

    constructor(props) {
        super(props);
        this.state = {
            editDetailsModal: false,
            shareDetailsModal: false,
            deleteModelModal: false,
            openLockedModal:false
        }

        this.updateModel = this.updateModel.bind(this);
        this.deleteModel = this.deleteModel.bind(this);
        this.clickPanelAction = this.clickPanelAction.bind(this);
        this.takeControl = this.takeControl.bind(this);
    }

    render() {
        let { model, models } = this.props;
        let temp = new Date(model.modified);
        let mod = new Date(model.modified);
        let temp3 = new Date().setHours(0, 0, 0, 0);
        let modDate = (temp.setHours(0, 0, 0, 0) === temp3) ? "today " + mod.toLocaleTimeString() : mod.toLocaleDateString();

        return (
            <React.Fragment>
                <Modal show={this.state.openLockedModal}>
                    <Modal.Header>
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
                <Col sm={3} md={3} onClick = {e => this.clickPanelAction(e)}>
                    <Panel className="recent-card">
                        <PanelHeading >
                            <OverlayTrigger
                                delayShow={Constants.TOOLTIP_DELAY}
                                placement="right"
                                overlay={
                                    <Popover
                                        id="model-title-popover"
                                        className={"tooltip-overlay"}
                                    >
                                        <span>
                                            <strong>{model.name}</strong>
                                        </span>
                                    </Popover>
                                }
                            >
                                <h3 className="model-name">{model.name}</h3>
                            </OverlayTrigger>
                        </PanelHeading>
                        <PanelBody>
                            <div>
                                Last modified by <b>{model.modifiedBy}</b>{" " + modDate}
                            </div>
                        </PanelBody>
                        <PanelFooter>
                            <ButtonToolbar className="card-btn-group">
                                {
                                    model.canBeEdited ?
                                        <Button className="icon-button" onClick={e => this.clickEditAction(e)}>
                                            <OverlayTrigger
                                                placement="top"
                                                delay={Constants.TOOLTIP_DELAY}
                                                overlay={
                                                    <Tooltip id="tooltip-edit">Edit Model</Tooltip>
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
                                        <Button className="icon-button" onClick={e => this.shareModel(e)}>
                                            <OverlayTrigger
                                                placement="top"
                                                delay={Constants.TOOLTIP_DELAY}
                                                overlay={
                                                    <Tooltip id="tooltip-share">Share Model</Tooltip>
                                                }
                                            >
                                                <span className="fa fa-share" />
                                            </OverlayTrigger>
                                        </Button>
                                    :
                                        null
                                }
                                <Button className="icon-button" onClick={e => this.clickViewAction(e)} >
                                    <OverlayTrigger
                                        placement="top"
                                        delay={Constants.TOOLTIP_DELAY}
                                        overlay={
                                            <Tooltip id="tooltip-view">View Model</Tooltip>
                                        }
                                    >
                                        <span className="fa fa-eye" />
                                    </OverlayTrigger>
                                </Button>
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
                                                    <span>Edit Model in New Tab</span>
                                                </div>
                                            </MenuItem>
                                        :
                                            null
                                    }
                                    <MenuItem onClick={e => this.clickViewAction(e, true)}>
                                        <div className="card-dropdown">
                                            <span className="fa fa-eye "></span>
                                            <span>View Model in New Tab</span>
                                        </div>
                                    </MenuItem>
                                    {
                                        model.canBeEdited ?
                                            <MenuItem onClick={e => this.editDetails(e)}>
                                                <div className="card-dropdown">
                                                    <span className="fa fa-pencil-square-o "></span>
                                                    <span>Edit Details</span>
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
                                            <span>Export Full Model</span>
                                        </div>
                                    </MenuItem>
                                    <MenuItem onClick={e => {
                                        e.stopPropagation();
                                        this.props.dispatch(saveDownload("./models/" + model["id"] + "/exportAsserted"));
                                    }}>
                                        <div className="card-dropdown">
                                            <span className="fa fa-download"></span>
                                            <span>Export Assets Only</span>
                                        </div>
                                    </MenuItem>
                                    <MenuItem onClick={e => callCopyModel(e, model, models, this.props.dispatch)}>
                                        <div className="card-dropdown">
                                            <span className="fa fa-copy"></span>
                                            <span>Copy Model</span>
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
                        </PanelFooter>
                    </Panel>
                </Col>
                <EditModelModal show={this.state.editDetailsModal} model={model} updateModel={this.updateModel}
                    onHide={() => this.setState({ ...this.state, editDetailsModal: false })} />

                {/* this looks silly, but we need to prevent the ShareModelModal mounting if the model cannot be shared because when it mounts it tries to call /authz */}
                {this.state.shareDetailsModal ? <ShareModelModal show={this.state.shareDetailsModal} model={model} dispatch={this.props.dispatch}
                    onHide={() => this.setState({ ...this.state, shareDetailsModal: false })} /> : null }

                <DeleteModelModal show={this.state.deleteModelModal} model={model} deleteModel={this.deleteModel}
                    onHide={() => this.setState({ ...this.state, deleteModelModal: false })} />
            </React.Fragment>
        );
    }

    updateModel(id, updatedModel) {
        this.setState({ ...this.state, editDetailsModal: false });
        this.props.dispatch(updateModel(id, updatedModel));
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

    updateModel(id, updatedModel) {
        this.setState({ ...this.state, editDetailsModal: false });
        this.props.dispatch(updateModel(id, updatedModel));
    }

    deleteModel(id) {
        this.setState({ ...this.state, deleteModelModal: false });
        this.props.dispatch(deleteModel(id));
    }
    
    editDetails(e) {
        e.stopPropagation();
        this.setState({ editDetailsModal: !this.state.editDetailsModal });
    }

    shareModel(e) {
        e.stopPropagation();
        this.setState({ shareDetailsModal: !this.state.shareDetailsModal });
    }

    deleteModelModal(e) {
        e.stopPropagation();
        this.setState({ deleteModelModal: !this.state.deleteModelModal });
    }
}

export default RecentCard;
