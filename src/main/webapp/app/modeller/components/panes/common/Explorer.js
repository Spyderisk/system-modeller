import React from "react";
import PropTypes from 'prop-types';
import {Rnd} from "../../../../../node_modules/react-rnd/lib/index";
import {bringToFrontWindow} from "../../../actions/ViewActions";
import {openDocumentation} from "../../../../common/documentation/documentation"

class Explorer extends React.Component {

    constructor(props) {
        super(props);

        this.renderRnd = this.renderRnd.bind(this);

        this.state = {
        }
    }

    shouldComponentUpdate(nextProps, nextState) {
        if ((!this.props.show) && (!nextProps.show)) {
            return false;
        }

        if (nextProps.loading.model) {
            return false;
        }

        return true;
    }

    renderRnd() {
        let {xScale, width, height} = this.props.rndParams

        return (
            <Rnd bounds={ '#view-boundary' }
                 default={{
                     x: window.outerWidth * xScale,
                     y: (100 / window.innerHeight) * window.devicePixelRatio,
                     width: width,
                     height: height,
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
                 <div className="explorer">
                      <button className="header"
                          onMouseDown={() => {
                              this.props.dispatch(bringToFrontWindow(this.props.windowName));
                          }}
                      >
                          <div className="header header-no-padding">
                              <h1>
                                  <div className={"doc-help-explorer"}>
                                      <div className="title">
                                          {this.props.title}
                                      </div>
                                  </div>
                              </h1>
                              <span className="button">
                                  <button onClick={e => this.props.onHide()}>
                                      <i className="fa fa-times"/>
                                  </button>
                              </span>
                              <span className="button">
                                  <button onClick={e => openDocumentation(e, this.props.documentationLink)}>
                                      <i className="fa fa-question"/>
                                  </button>
                              </span>
                          </div>
                      </button>

                      {this.props.renderContent()}
                 </div>
            </Rnd>
        );
    }
    
    render() {
        if (!this.props.show) {
            return null;
        }

        return (this.renderRnd())
    }   
}

Explorer.propTypes = {
    title: PropTypes.string,
    windowName: PropTypes.string,
    documentationLink: PropTypes.string,
    rndParams: PropTypes.object,
    selectedAsset: PropTypes.object,
    isActive: PropTypes.bool, // is in front of other panels
    show: PropTypes.bool,
    onHide: PropTypes.func,
    loading: PropTypes.object,
    dispatch: PropTypes.func,
    renderContent: PropTypes.func,
    windowOrder: PropTypes.number,
};

export default Explorer;
