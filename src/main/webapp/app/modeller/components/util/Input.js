import React from "react";
import PropTypes from 'prop-types';

class Input extends React.Component {

    shouldComponentUpdate(nextProps) {
        return nextProps.shouldUpdate;
    }

    render() {
        return (
            <input className={this.props.style}
                   type={this.props.type}
                   value={this.props.value}
                   checked={this.props.checked}
                   onChange={this.props.onChange}/>
        )
    }
}

Input.propTypes = {
    type: PropTypes.string,
    style: PropTypes.string,
    shouldUpdate: PropTypes.bool,
    value: PropTypes.any,
    checked: PropTypes.bool,
    onChange: PropTypes.func
};

export default Input;