import React from "react";
import PropTypes from 'prop-types';
import {Rnd} from "../../../../../node_modules/react-rnd/lib/index";
import {bringToFrontWindow} from "../../../actions/ViewActions";
import {connect} from "react-redux";
import {openDocumentation} from "../../../../common/documentation/documentation"
import {JsonView, defaultStyles} from 'react-json-view-lite';
import 'react-json-view-lite/dist/index.css';

class RecommendationsExplorer extends React.Component {

    constructor(props) {
        super(props);

        this.rnd = null;

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
        const {model, dispatch, loading, ...modalProps} = this.props;

        if (!this.props.show) {
            return null;
        }

        let recommendations = this.props.recommendations;

        return (
          <Rnd ref={ c => {this.rnd = c;} }
               bounds={ '#view-boundary' }
               default={{
                   x: window.outerWidth * 0.20,
                   y: (100 / window.innerHeight) * window.devicePixelRatio,
                   width: 700,
                   height: 600,
               }}
               style={{ zIndex: this.props.windowOrder }}
               minWidth={150}
               minHeight={200}
               cancel={".content, .text-primary, strong, span"}
               onResize={(e) => {
                   if (e.stopPropagation) e.stopPropagation();
                   if (e.preventDefault) e.preventDefault();
                   e.cancelBubble = true;
                   e.returnValue = false;
               }}
               onDrag={(e) => {
                   if (e.stopPropagation) e.stopPropagation();
                   if (e.preventDefault) e.preventDefault();
                   e.cancelBubble = true;
                   e.returnValue = false;
               }}
               className={!this.props.show ? "hidden" : null}>
               <div className="recommendations-explorer">

                   <div className="header" onMouseDown={() => {
                       this.props.dispatch(bringToFrontWindow("recommendationsExplorer"));
                   }}>
                       <h1>
                           <div className={"doc-help-explorer"}>
                               <div>
                                   {"Recommendations Explorer"}
                               </div>
                           </div>
                       </h1>
                       <span classID="rel-delete"
                             className="menu-close fa fa-times"
                             onClick={() => {
                                 this.props.onHide();
                             }}>
                       </span>
                       <span className="menu-close fa fa-question" 
                             onClick={e => openDocumentation(e, "redirect/recommendations-explorer")} />
                   </div>

                    <div className="content">
                        {recommendations && <JsonView data={recommendations} shouldExpandNode={shouldExpandRecommendationsNode} style={defaultStyles} />}
                   </div>
               </div>

          </Rnd>
        );
    }

}

function shouldExpandRecommendationsNode(level) {
    return level <= 1;
}

RecommendationsExplorer.propTypes = {
    selectedAsset: PropTypes.object,
    isActive: PropTypes.bool, // is in front of other panels
    model: PropTypes.object,
    recommendations: PropTypes.object,
    show: PropTypes.bool,
    onHide: PropTypes.func,
    loading: PropTypes.object,
    dispatch: PropTypes.func,
    authz: PropTypes.object,
    windowOrder: PropTypes.object,
};

let mapStateToProps = function (state) {
    return {
        windowOrder: state.view["recommendationsExplorer"]
    }
};

export default connect(mapStateToProps)(RecommendationsExplorer);
