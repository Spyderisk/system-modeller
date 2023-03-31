import React from "react";
import ReactDOM from "react-dom";
import { axiosInstance } from "../../../common/rest/rest";
import { Button, Modal } from "react-bootstrap";
import { copyModel } from "../../actions/api";

class LockedModal extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            showModal: true
        };
    }

    render() {
        let modelId = $("meta[name='_model']").attr("content");

        return (
            <Modal show={this.state.showModal}>
                <Modal.Header >
                    <Modal.Title>Locked Model</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <div className="input-form">
                        <h4>
                            The model is being edited by another user.
                        </h4>
                        <Button
                            style={{ marginRight: "5px" }}
                            bsStyle="primary"
                            onClick={() => {
                                window.location.replace(process.env.config.API_END_POINT + "/models/" + modelId + "/read");
                                this.setState({
                                    showModal: false
                                });

                            }}
                            className={"search-sort-btn2"}
                        >
                            <span className="fa fa-eye" />{" Open in view-only mode"}
                        </Button>
                        <Button
                            style={{ marginRight: "5px" }}
                            bsStyle="primary"
                            onClick={() => {
                                axiosInstance.post("/models/" + modelId + "/checkout")
                                    .then(() => {
                                        window.location.replace(process.env.config.API_END_POINT + "/models/" + modelId + "/edit");
                                    })
                                this.setState({
                                    showModal: false
                                });

                            }}
                            className={"search-sort-btn2"}
                        >
                            <span className="fa fa-pencil" />{" Take control"}
                        </Button>
                    </div>
                </Modal.Body>
            </Modal>
        )
    }
}

export function lockedModel(modalId) {
    //Renders the LockedModal Component inside the canvas from the canvas id
    //The LockedModal component has state for future implementations of Cancel button
    ReactDOM.render(
        <LockedModal />,
        document.getElementById("canvas-container")
    );
}

export function callCopyModel(e, model, models, dispatch) {

    e.stopPropagation();

    // Escape the existing model's name so that special regex characters are ignored.
    let modelName = model.name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    
    // Find models that look like they are copies of the model being copied (have e.g. " (1)" appended).
    let regex = new RegExp(modelName + " \\(\\d+\\)$");
    let filteredModel = models.filter(m => regex.test(m.name))

    // Extract all the numbers of the existing copies.
    var numbers = filteredModel.map((m) => {
        return parseInt(m.name.substring(m.name.lastIndexOf("(") + 1, m.name.lastIndexOf(")")));
    })

    // Find the largest number.
    let max = numbers.reduce(function (a, b) {
        return Math.max(a, b);
    }, 0);

    // Construct the new model name and the DTO for the copy.
    let num = max + 1
    let copyName = model.name + " (" + num + ")";
    let copyModelDto = { ...model, name: copyName };

    dispatch(copyModel(model.id, copyModelDto));
}
