import PropTypes from "prop-types";
import React, { Component } from "react";
import { Button, Col, Grid, Modal, ProgressBar, Row } from "react-bootstrap";
import ReactDOM from "react-dom";
import { connect } from "react-redux";
import Banner from "../../common/components/banner/Banner";
import { getDomains, getUsers, toggleUploadModal, updateUploadProgress, uploadDomain } from "../actions/api";
import "../styles/index.scss";
import AccessEditor from "./AccessEditor";
import Domain from "./Domain";

class DomainManager extends Component {

    constructor(props) {
        super(props);

        this.state = {
            uploadModal: {
                isOpen: false,
                domain: undefined,
                domainUri: undefined,
                newDomain: false,
            },
            accessListModal: {
                isOpen: false,
                domain: undefined,
                domainUri: undefined,
                userList: [],
                activeList: []
            }
        };

        this.toggleModal = this.toggleModal.bind(this);
        this.toggleAccessListModal = this.toggleAccessListModal.bind(this);
        this.handleDomainSubmit = this.handleDomainSubmit.bind(this);
    }

    componentDidMount() {
        this.props.dispatch(getDomains());
        this.props.dispatch(getUsers());
    }
    
    componentWillReceiveProps(nextProps) {
        //console.log("componentWillReceiveProps:",this.props, nextProps);
        this.setState({
            ...this.state,
            uploadModal: {
                isOpen: nextProps.upload.isOpen,
                domain: nextProps.upload.domain,
                domainUri: nextProps.upload.domainUri,
                newDomain: nextProps.upload.newDomain
            },
            accessListModal: {
                ...this.state.accessListModal,
                userList: nextProps.userList,
                activeList: nextProps.activeList
            }
        });
    }

    render() {
        let {ontologies} = this.props;
        let domainUris = Object.keys(ontologies);
        let uploadStatus = this.props.upload.status;
        let uploadFailureMessage = uploadStatus !== undefined && uploadStatus.failed ?
            typeof uploadStatus.reason === "object" ? "ERROR: " + uploadStatus.reason.message : "ERROR: " + uploadStatus.reason : "";

        let modalTitle = (this.state.uploadModal.newDomain ? "New" : "Update") + " Knowledgebase";

        if (!this.state.uploadModal.newDomain) {
            modalTitle += " - " + (this.state.uploadModal.domain ? 
                (this.state.uploadModal.domain.label !== this.state.uploadModal.domain.title) ? this.state.uploadModal.domain.label + " (" + this.state.uploadModal.domain.title + ")" :
                this.state.uploadModal.domain.label : "");
        }

        return (
            <div className="content domain-manager">
                <div className="domain-manager-container">
                    <Banner title="Knowledgebase Manager" options={[
                    ]}/>
                    <div className="domains">
                            {domainUris.map((a, index) => {
                                let o = ontologies[a];
                                return <Domain key={index} ontology={o} domainUri={a}
                                               toggleModal={this.toggleModal}
                                               toggleAccessListModal={this.toggleAccessListModal}
                                               dispatch={this.props.dispatch}/>
                            })}
                    </div>
                    <div>
                        <Button bsStyle="primary"
                                onClick={() => this.toggleModal(true, undefined, undefined, true)}
                        >Upload new knowledgebase</Button>
                    </div>
                </div>
                
                <AccessEditor 
                accessListModal={this.state.accessListModal}
                onHide={() => this.toggleAccessListModal(false, undefined, undefined)}
                dispatch={this.props.dispatch} userList={[]}/>
                
                <Modal backdrop={true} show={this.state.uploadModal.isOpen}
                       onHide={() => this.toggleModal(false, undefined, undefined, false)}>
                    <Modal.Header closeButton>
                        <Modal.Title>{modalTitle}</Modal.Title>
                    </Modal.Header>
                    <Modal.Body>
                        {this.state.uploadModal.newDomain ?
                        <Grid fluid>
                            <Row>
                                <Col xs={6}><span className="text-bold">New Domain bundle (*.zip): </span></Col>
                                <Col xs={6}><input placeholder="file" type="file" accept=".zip"
                                                   ref="file-upload"/></Col>
                            </Row>
                        </Grid>
                        :
                        <Grid fluid>
                            <Row>
                                <Col xs={6}><span className="text-bold">New Domain file/bundle (*.rdf, *.rdf.gz, *.nq, *.nq.gz, *.zip): </span></Col>
                                <Col xs={6}><input placeholder="file" type="file" accept=".rdf,.rdf.gz,.nq,.nq.gz,.zip"
                                                   ref="file-upload"/></Col>
                            </Row>
                            <Row>
                                <Col xs={6}><span className="text-bold">New ontologies.json (optional): </span></Col>
                                <Col xs={6}><input placeholder="file" type="file" accept=".json"
                                                   ref="file2-upload"/></Col>
                            </Row>
                        </Grid>
                        }
                        <hr/>
                        <ProgressBar active now={this.props.upload.progress}/>
                        {this.props.upload.progress === 100 || this.props.upload.completed ?
                            <div>
                                <span className="text-primary" style={{fontWeight: "bold"}}>{"Import completed!"}</span>
                            </div>
                            :
                            null
                        }
                        {uploadStatus !== undefined && uploadStatus.failed ?
                            <div>
                                <span className="text-danger"
                                      style={{fontWeight: "bold"}}>{uploadFailureMessage}</span>
                            </div>
                            :
                            null
                        }
                    </Modal.Body>
                    <Modal.Footer>
                        <div>
                            <Button bsStyle="primary" onClick={() => this.toggleModal(false, undefined, undefined, false)}>Close</Button>
                            <Button bsStyle="success" onClick={this.handleDomainSubmit}>Upload</Button>
                        </div>
                    </Modal.Footer>
                </Modal>
            </div>
        );
    }

    toggleModal(flag, domainUri, domain, newDomain) {
        this.props.dispatch(toggleUploadModal(flag, domainUri, domain, newDomain));
    }

    toggleAccessListModal(flag, domainUri, domain) {
        //console.log("toggleAccessListModal:", flag, domainUri, domain);
        this.setState({
            ...this.state,
            accessListModal: {
                ...this.state.accessListModal,
                isOpen: flag,
                domain: domain,
                domainUri: domainUri
            }
        });
    }
    
    handleDomainSubmit() {
        const data = new FormData();
        const file = ReactDOM.findDOMNode(this.refs["file-upload"]).files[0];
        const file2 = this.state.uploadModal.newDomain ? undefined : ReactDOM.findDOMNode(this.refs["file2-upload"]).files[0];

        let domainUri = (this.state.uploadModal.domainUri !== undefined) ? 
            this.state.uploadModal.domainUri : "";
        let newDomain = this.state.uploadModal.newDomain;

        if (file !== undefined) {
            data.append("file", file);
            data.append("domainUri", domainUri);
            data.append("newDomain", newDomain);
            if (file2 !== undefined) {
                data.append("file2", file2);
            }

            this.props.dispatch(updateUploadProgress(10));
            this.props.dispatch(uploadDomain(data, true));
        }
        else {
            console.log("WARNING: file is undefined");
            alert("Please choose a file!");
        }
    }
}

let mapStateToProps = function (state) {
    return {
        ontologies: state.domainManager.ontologies,
        upload: state.domainManager.upload,
        download: state.domainManager.download,
        userList: state.domainManager.userList,
        activeList: state.domainManager.activeList
    };
};

DomainManager.propTypes = {
    dispatch: PropTypes.func,
    ontologies: PropTypes.object,
    upload: PropTypes.object,
    download: PropTypes.object,
    activePalette: PropTypes.object,
    userList: PropTypes.array,
    activeList: PropTypes.array
};

export default connect(mapStateToProps)(DomainManager);
