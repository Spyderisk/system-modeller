import React from "react";
import PropTypes from "prop-types";
import {Button, Well, Thumbnail} from "react-bootstrap";

import {uploadScreenshot} from "../../../../../modeller/actions/ModellerActions";
import * as instr from "../../../../modellerConstants";

class ScreenshotOption extends React.Component {

    constructor(props) {
        super(props);

        this.handleImageSubmit = this.handleImageSubmit.bind(this);

        this.state = {
            screenshotAvailable: false,
            takingScreenshot: false
        }
    }

    componentWillMount() {
        let screenshotAvailable = this.isScreenshotAvailable();
        this.setState({
            ...this.state,
            screenshotAvailable: screenshotAvailable
        });
    }

    render() {
        let screenshotAvailable = this.isScreenshotAvailable();

        let screenshotContent = "";
        if (this.state.screenshotAvailable) {
            let imageLink = process.env.config['API_END_POINT'] + "/models/" + this.props.modelId + "/screenshot";
            screenshotContent = <div>
                <Thumbnail
                    key={"model-screenshot-" + this.props.modelId}
                    href={imageLink}
                    src={imageLink}
                    alt="Screenshot of the current model"
                    onClick={(event) => {
                        event.preventDefault();
                        window.open(imageLink);
                    }}
                />
                <hr/>
            </div>
        }
        return (
            <Well className="option-well">
                <h4>{"Generate Screenshot"}</h4>
                <hr/>
                {
                    screenshotContent
                }
                <Button bsStyle={this.state.takingScreenshot ? "default" : "primary"}
                        disabled={this.state.takingScreenshot}
                        onClick={this.handleImageSubmit}>{this.state.takingScreenshot ? "Taking screenshot..." : screenshotAvailable ? "Re-take screenshot" : "Create screenshot"}</Button><br/>
                <p className="text-danger">{screenshotAvailable ? "Warning: Doing this will overwrite previous screenshot!" : ""}</p>
                <p className="text-primary">You may need to re-open this modal to see the new image.</p>
            </Well>
        );
    }

    handleImageSubmit() {
        this.setState({
            ...this.state,
            takingScreenshot: true,
            screenshotAvailable: false
        });
        uploadScreenshot(this.props.modelId)
            .then((response) => {
                this.props.dispatch({
                    type: instr.UPLOAD_SCREENSHOT,
                    payload: response
                });
                this.setState({
                    ...this.state,
                    takingScreenshot: false,
                    screenshotAvailable: true
                });
            });
    }

    isScreenshotAvailable() {
        //STW: if screenshots are ever resurrected this MUST be converted to use axios like all the other requests.
        //Commenting the body out to force someone to at least read this comment.
/*
        try {
            let http = new XMLHttpRequest();
            const screenshotLocation = process.env.config['API_END_POINT'] + "/models/" + this.props.modelId + "/screenshot";
            http.open("HEAD", screenshotLocation, false);
            http.send();
            return http.status !== 404;
        } catch (err) {
            return false;
        }
*/
    }

}

ScreenshotOption.propTypes = {
    modelId: PropTypes.string,
    upload: PropTypes.object,
    dispatch: PropTypes.func,
};

export default ScreenshotOption;
