import React, {Component} from "react";
import PropTypes from 'prop-types';
import './SlidingPanelHandle.scss';

class SlidingPanelHandle extends Component {

    constructor(props) {
        super(props);

        this.state = {dragging: false};

        this.handleMouseDrag = this.handleMouseDrag.bind(this);
        this.handleMouseDown = this.handleMouseDown.bind(this);
        this.handleMouseUp = this.handleMouseUp.bind(this);
    }

    handleMouseDrag(e) {
        if (!this.state.dragging) {
            return;
        }
        this.props.resizePanelWidth(this.props.isLeft ? e.pageX : $(document).outerWidth() - e.pageX);
    }

    handleMouseDown(e) {
        this.setState({...this.state, dragging: true});
        // prevent propogation to stop triggering events on the panel titles
        e.stopPropagation();
        e.preventDefault();
        e.target.setPointerCapture(e.pointerId);
    }

    handleMouseUp(e) {
        this.setState({...this.state, dragging: false});
        // prevent propogation to stop triggering events on the panel titles
        e.stopPropagation();
        e.preventDefault();
        e.target.releasePointerCapture(e.pointerId)
    }

    render() {
        return (<div
            className={"ssm-sliding-panel-handle " + (this.props.isLeft ? "left" : "right")}
            onPointerDown={this.handleMouseDown}
            onPointerMove={this.handleMouseDrag}
            onPointerUp={this.handleMouseUp}
        />);
    }

}

SlidingPanelHandle.defaultProps = {
    isLeft: true,
    isVisible: false,
    title: 'empty'
};

SlidingPanelHandle.propTypes = {
    isLeft: PropTypes.bool.isRequired,
    isVisible: PropTypes.bool,
    title: PropTypes.node,
    resizePanelWidth: PropTypes.func
};

export default SlidingPanelHandle;
