import React from "react";
import PropTypes from 'prop-types';

import {
    Modal,
    Tab,
    Row,
    Col,
    Nav,
    NavItem,
} from "react-bootstrap";

import InformationOption from "./InformationOption";
import ScreenshotOption from "./ScreenshotOption";
import ViewLayerOption from "./ViewLayerOption";
import ExportOption from "./ExportOption";

/**
 * This class displays a menu in a modal, containing layer options,
 * screenshot options, and any other additional features.
 */
class OptionsModal extends React.Component {

    constructor(props) {
        super(props);

    }

    render() {
        return (
            <div>
                <Modal {...this.props} backdrop={true} bsSize="large">
                    <Modal.Header closeButton>
                        <Modal.Title>{"Configure Model"}</Modal.Title>
                    </Modal.Header>

                    <Modal.Body>
                        <Tab.Container id="options-nav" defaultActiveKey="0">
                            <Row className="clearfix">
                                <Col sm={3}>
                                    <Nav bsStyle="pills" stacked>
                                        <NavItem eventKey="0">
                                            Model Information
                                        </NavItem>
                                        {/* <NavItem eventKey="1">
                                            Select View Layer
                                        </NavItem> */}
                                        {/* <NavItem eventKey="2">
                                            Generate Screenshot
                                        </NavItem> */}
                                        <NavItem eventKey="3">
                                            Export Model
                                        </NavItem>
                                    </Nav>
                                </Col>
                                <Col sm={9}>
                                    <Tab.Content animation mountOnEnter={true}>
                                        <Tab.Pane eventKey="0">
                                            <InformationOption model={this.props.model} />
                                        </Tab.Pane>
                                        {/* <Tab.Pane eventKey="1">
                                            <ViewLayerOption modelId={this.props.model.id}
                                                              layers={this.props.layers}
                                                             selectedLayers={this.props.selectedLayers}
                                                              dispatch={this.props.dispatch}/>
                                        </Tab.Pane> */}
                                        {/* <Tab.Pane eventKey="2">
                                        <Tab.Pane eventKey="2">
                                            <ScreenshotOption modelId={this.props.model.id}
                                                              dispatch={this.props.dispatch}/>
                                        </Tab.Pane> */}
                                        <Tab.Pane eventKey="3">
                                            <ExportOption modelId={this.props.model.id} />
                                        </Tab.Pane>
                                    </Tab.Content>
                                </Col>
                            </Row>
                        </Tab.Container>
                    </Modal.Body>
                </Modal>
            </div>
        );
    }

}

OptionsModal.propTypes = {
    model: PropTypes.object,
    layers: PropTypes.array,
    selectedLayers: PropTypes.array,
    dispatch: PropTypes.func,
    onHide: PropTypes.func
};

export default OptionsModal;