import React from "react";
import PropTypes from "prop-types";
import ReactDOM from "react-dom"
import {Modal, Button, ProgressBar, Grid, Row, Col} from "react-bootstrap"
import {updateUploadProgress, uploadModel} from "../../actions/api";

class ImportModelModal extends React.Component {

    constructor(props) {
        super(props);

        this.onModalShown = this.onModalShown.bind(this);
        this.onHideModal = this.onHideModal.bind(this);
        this.fileChanged = this.fileChanged.bind(this);
        this.handleSubmit = this.handleSubmit.bind(this);
        this.handleKeyDown = this.handleKeyDown.bind(this);
    }

    componentWillMount() {
        //console.log("componentWillMount");
        this.setState({
            fileChooserDisabled: false,
            importButtonDisabled: true,
            fileName: ""
        });
        //console.log("importButtonDisabled = " + this.state.importButtonDisabled);
    }

    componentWillReceiveProps(nextProps) {
        //console.log("componentWillReceiveProps:");
        //console.log(nextProps);
        if (nextProps.upload.progress > 0) {
            //console.log("Upload in progress - disabling import button");
            if (!this.state.importButtonDisabled) {
                this.setState({
                    ...this.state,
                    fileChooserDisabled: false,
                    importButtonDisabled: true
                });
            }
            //else {
            //    console.log("(button was already disabled)");
            //}
        }
        if (nextProps.upload.completed) {
            // $("#import-model-dialog").addClass("hidden");
            setTimeout(() => {
                this.onHideModal();
            }, 1000)
        }
    }

    componentDidUpdate() {
        //console.log("componentDidUpdate");
        //console.log(this.state);
        //console.log(this.props);
    }

    getErrorMsg(status) {
        let error = "unknown";

        //"reason" may be an object or a simple string
        if (status?.reason) {
            error = status.reason.message ? status.reason.message : status.reason;
        }

        return "ERROR: " + error;
    }

    render() {
        let uploadFailed = this.props.upload.status?.failed;
        let errorMsg = uploadFailed ? this.getErrorMsg(this.props.upload.status) : "";

        return (
            <div className={"import-model-dialog"}>
            <Modal show={this.props.show} onHide={this.onHideModal} backdrop={true} onEnter={this.onModalShown}
                   onKeyDown={this.handleKeyDown}>
                <Modal.Header closeButton>
                    <Modal.Title>Import Existing Model</Modal.Title>
                </Modal.Header>

                <Modal.Body>
                    <div style={{width: "100%"}}>
                        <Grid fluid>
                            <Row>
                                <Col xs={12}>
                                    <div className="input-group">
                                        <input ref="file-upload" type="file" accept=".nq,.nq.gz"
                                               style={{display: "none"}} onChange={this.fileChanged}/>
                                        <button className="btn btn-primary" onClick={() => {
                                            this.refs["file-upload"].click();
                                            return false;
                                        }}
                                                disabled={this.state.fileChooserDisabled}>
                                            Browse
                                        </button>
                                        <span id="file-name" style={{marginLeft: "10px"}}>
                                            { this.state.fileName || "No file chosen" }
                                        </span>
                                    </div>
                                </Col>
                            </Row>
                            <hr/>
                            <Row>
                                <Col xs={7}>
                                    <span>Import asserted facts only?</span>
                                </Col>
                                <Col xs={1}>
                                    <input ref="file-upload-asserted" type="checkbox"/>
                                </Col>
                            </Row>
                            <Row>
                                <Col xs={7}>
                                <span>
                                    Overwrite existing model?&nbsp;
                                    <span className="text-danger">Cannot be undone! Make sure to take a backup!</span>
                                </span>
                                </Col>
                                <Col xs={1}>
                                    <input ref="file-upload-overwrite" type="checkbox"/>
                                </Col>
                            </Row>
                            <Row>
                                <Col xs={7}>
                                <span>
                                    New name?&nbsp;
                                </span>
                                </Col>
                                <Col xs={1}>
                                    <input ref="file-upload-new-name-check" type="checkbox" id="new-name"
                                    onClick={() => {
                                        (ReactDOM.findDOMNode(this.refs["file-upload-new-name"]).disabled =
                                            !ReactDOM.findDOMNode(this.refs["file-upload-new-name-check"]).checked)
                                    }}/>
                                </Col>
                                <Col xs={4}>
                                    <input ref="file-upload-new-name" style={{width: "100%"}} type="text" disabled={true}/>
                                </Col>
                            </Row>
                        </Grid>
                    </div>
                    <hr/>
                    <ProgressBar active now={this.props.upload.progress}/>
                    {this.props.upload.progress === 100 || this.props.upload.completed ?
                        <div>
                            <span className="text-primary" style={{fontWeight: "bold"}}>{"Import completed!"}</span>
                        </div>
                        :
                        null
                    }
                    {uploadFailed ?
                        <div>
                            <span className="text-danger" style={{fontWeight: "bold"}}>{errorMsg}</span>
                        </div>
                        :
                        null
                    }
                </Modal.Body>

                <Modal.Footer>
                    <Button onClick={this.onHideModal}>Cancel</Button>
                    <Button bsStyle="primary" disabled={this.state.importButtonDisabled}
                            onClick={this.handleSubmit} id={"import-btn"}>Import</Button>
                </Modal.Footer>
            </Modal>
            </div>
        );
    }

    onModalShown() {
        //console.log( "onModalShown" );
        this.setState({
            ...this.state,
            fileChooserDisabled: false,
            importButtonDisabled: true
        });
    }

    fileChanged() {
        //console.log("file changed");
        var file = ReactDOM.findDOMNode(this.refs["file-upload"]).files[0];

        //console.log("file = " + file);
        if (file !== undefined) {
            this.setState({
                ...this.state,
                fileName: file.name,
                importButtonDisabled: false
            });
        }
        else {
            //console.log("no file defined (set import button to disabled)");
            this.setState({
                ...this.state,
                importButtonDisabled: true
            });
        }
    }

    handleSubmit() {
        //console.log("Importing existing model");

        var data = new FormData();
        var file = ReactDOM.findDOMNode(this.refs["file-upload"]).files[0];
        //console.log("file = " + file);

        if (file !== undefined) {
            data.append("file", file);
            data.append("asserted", ReactDOM.findDOMNode(this.refs["file-upload-asserted"]).checked);
            data.append("overwrite", ReactDOM.findDOMNode(this.refs["file-upload-overwrite"]).checked);
            if(ReactDOM.findDOMNode(this.refs["file-upload-new-name-check"]).checked) {
                data.append("newName", ReactDOM.findDOMNode(this.refs["file-upload-new-name"]).value);
            }

            this.props.dispatch(updateUploadProgress(10));
            this.props.dispatch(uploadModel(data, true));
        }
        else {
            console.log("WARNING: file is undefined");
            alert("Please choose a file!");
        }
    }

    handleKeyDown(event) {
        if (event.keyCode === 27) {
            event.preventDefault();
            this.onHideModal();
        }
        else if (event.keyCode === 13) {
            $("#import-btn").click();
            event.preventDefault();
        }
    }

    onHideModal() {
        //console.log("onHideModal");
        this.props.dispatch(updateUploadProgress(0)); // reset progress bar

        this.props.onHideImportModal(); // call parent to hide window
        this.setState({
            ...this.state,
            fileName: ""
        })
    }

}

ImportModelModal.propTypes = {
    upload: PropTypes.object,
    dispatch: PropTypes.func,
    onHideImportModal: PropTypes.func
};

export default ImportModelModal;
