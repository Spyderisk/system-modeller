import React, {Component} from "react";
import PropTypes from "prop-types";

import "./Modal.scss";

class Modal extends Component {
	render() {
		return (
			<div className="ss-modal" onClick={this.props.onClose}>
				<div
					className={"ss-modal-container" + (this.props.small ? " small" : "")  + (this.props.big ? " big" : "")}
					onClick={e => {
						e.preventDefault();
						e.stopPropagation();
					}}>
					<div className="ss-modal-header">
						<div className="ss-left">
							<h1>{this.props.title}</h1>
						</div>
						<div className="ss-right">
							<span className="ss-modal-close fa fa-close fa-lg" onClick={this.props.onClose} />
						</div>
					</div>
					<div className="ss-modal-content">
						{this.props.content}
					</div>
					<div className="ss-modal-content-separator" />
					<div className="ss-modal-footer">
						{this.props.footer ||
							<a className="ss-modal-button" onClick={this.props.onClose}>
								Close
							</a>}
					</div>
				</div>
			</div>
		);
	}
}

Modal.propTypes = {
	title: PropTypes.string,
	content: PropTypes.node.isRequired,
	footer: PropTypes.node,
	onClose: PropTypes.func.isRequired,
	small: PropTypes.bool,
	big: PropTypes.bool,
};

export default Modal;
