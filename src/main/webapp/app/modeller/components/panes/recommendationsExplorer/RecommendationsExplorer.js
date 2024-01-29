import React from "react";
import PropTypes from 'prop-types';
import {connect} from "react-redux";
import {JsonView, defaultStyles} from 'react-json-view-lite';
import 'react-json-view-lite/dist/index.css';
import Explorer from "../common/Explorer"

class RecommendationsExplorer extends React.Component {

    constructor(props) {
        super(props);

        this.renderContent = this.renderContent.bind(this);

        this.state = {
        }
    }

    shouldComponentUpdate(nextProps, nextState) {
        let shouldComponentUpdate = true;

        if ((!this.props.show) && (!nextProps.show)) {
            return false;
        }

        if (nextProps.loading.model) {
            return false;
        }

        if (this.props.isActive != nextProps.isActive) {
            return true;
        }

        if (this.props.selectedAsset != nextProps.selectedAsset) {
            return true;
        }

        return shouldComponentUpdate;
    }

    render() {
        if (!this.props.show) {
            return null;
        }

        let rndParams = {
            xScale: 0.20,
            width: 700,
            height: 600
        }
        
        return (
            <Explorer
                title={"Recommendations Explorer"}
                windowName={"recommendationsExplorer"}
                documentationLink={"redirect/recommendations-explorer"}
                rndParams={rndParams}
                selectedAsset={this.props.selectedAsset}
                isActive={this.props.isActive}
                show={this.props.show}
                onHide={this.props.onHide}
                loading={this.props.loading}
                dispatch={this.props.dispatch}
                renderContent={this.renderContent}
                windowOrder={this.props.windowOrder}
            />
        )
    }

    renderContent() {
        let recommendations = this.props.recommendations;
        return (
            <div className="content">
                {recommendations && <JsonView data={recommendations} shouldExpandNode={shouldExpandRecommendationsNode} style={defaultStyles} />}
            </div>
        )
    }
}

function shouldExpandRecommendationsNode(level) {
    return level <= 1;
}

RecommendationsExplorer.propTypes = {
    selectedAsset: PropTypes.object,
    isActive: PropTypes.bool, // is in front of other panels
    recommendations: PropTypes.object,
    show: PropTypes.bool,
    onHide: PropTypes.func,
    loading: PropTypes.object,
    dispatch: PropTypes.func,
    windowOrder: PropTypes.number,
};

let mapStateToProps = function (state) {
    return {
        windowOrder: state.view["recommendationsExplorer"]
    }
};

export default connect(mapStateToProps)(RecommendationsExplorer);
