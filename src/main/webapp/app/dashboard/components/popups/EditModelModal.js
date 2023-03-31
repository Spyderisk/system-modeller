import PropTypes from 'prop-types';
import React, { Component } from "react";
import { Button, Modal } from "react-bootstrap";
import { MODEL_DESCRIPTION_LIMIT, MODEL_NAME_LIMIT } from '../../../common/constants';

class EditModelModal extends Component {

    constructor(props) {
        super(props);

        this.state = {
            draftName: "",
            draftDescription: ""
        }
    }

    componentWillMount() {
        this.setState({
            ...this.state,
            draftName: this.props.model.name,
            draftDescription: this.props.model.description ? this.props.model.description : ""
        })
    }

    render() {
        const {model, updateModel, ...modalProps} = this.props;

        return (
            <Modal {...modalProps}>
                <Modal.Header closeButton>
                    <Modal.Title>Edit Model Name and Description</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <div>
                        <label htmlFor="input-name">Name:</label>
                        <input id="input-name"
                            tabIndex={1}
                            autoFocus
                            style={{
                                width: "100%",
                                padding: "10px",
                                backgroundColor: "#eee"
                            }} 
                            maxLength={MODEL_NAME_LIMIT}
                            value={this.state.draftName}
                            onChange={(e) => this.setState({
                                ...this.state,
                                draftName: e.nativeEvent.target.value
                            })}
                        />
                    </div>
                    <div style={{paddingTop:"15px"}}>
                        <label htmlFor="input-description">Description:</label>
                        <textarea id="input-description"
                            tabIndex={2}
                            style={{
                                width: "100%",
                                maxWidth: "100%",
                                minWidth: "100%",
                                padding: "10px",
                                backgroundColor: "#eee"
                            }}
                            maxLength={MODEL_DESCRIPTION_LIMIT}
                            value={this.state.draftDescription}
                            onChange={e => this.setState({
                                ...this.state,
                                draftDescription: e.nativeEvent.target.value
                            })}
                        />
                    </div>
                </Modal.Body>
                <Modal.Footer>
                    <Button
                        tabIndex={4}
                        onClick={this.props.onHide}>
                        Cancel
                    </Button>
                    <Button
                        tabIndex={3}
                        bsStyle="primary"
                        onClick={() => {
                            // remove new lines and trim whitespace from start and end
                            let updates = {
                                name: this.state.draftName.replace(/\r?\n|\r/g, " ").trim(),
                                description: this.state.draftDescription.replace(/\r?\n|\r/g, " ").trim()
                            }
                            updateModel(model.id, updates)
                        }}
                        disabled={(this.state.draftName.replace(/\r?\n|\r/g, " ").trim().length == 0)}>
                        Save
                    </Button>
                </Modal.Footer>
            </Modal>
        )
    }
}

EditModelModal.propTypes = {
    model: PropTypes.object,
    updateModel: PropTypes.func
};

export default EditModelModal;
