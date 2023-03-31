import React from "react";
import PropTypes from 'prop-types';

class RootCausePanel extends React.Component {

    constructor(props) {
        super(props);
    }

    render() {
        var self = this;
        var misbehaviours = [];

        if (self.props.loading) {
            console.log("Still loading...");
            return (<div className="container-fluid"><div className="row"><span className="col-md-12">Loading...</span></div></div>);
        }
        
        console.log("rootCause:");
        console.log(this.props.rootCause);

        if (this.props.rootCause) {
            misbehaviours = this.props.rootCause;
            if (!misbehaviours) {
                misbehaviours = [];
            }
        }

        if (misbehaviours.length > 0) {
            //Sort by asset (assetLabel) first, then by misbehaviour (misbehaviourLabel)
            misbehaviours = misbehaviours.sort(function(a,b) {
                                                    if (a.m.assetLabel === b.m.assetLabel) {
                                                        return (a.m.misbehaviourLabel < b.m.misbehaviourLabel) ? -1 : (a.m.misbehaviourLabel > b.m.misbehaviourLabel) ? 1 : 0;
                                                    }
                                                    else {
                                                        return (a.m.assetLabel < b.m.assetLabel) ? -1 : 1
                                                    }
                                               });
			return (
				<div className="container-fluid">
					{misbehaviours.map((misbehaviour) => {
						return (
							<div className={'row misbehaviour-' + (misbehaviour["active"]?"active":"inactive")}>
								<span className="col-md-12">
									<strong>
										{misbehaviour.m.misbehaviourLabel}
									</strong>
									    {" at "}
									<strong>
                                        {misbehaviour.m.assetLabel}
									</strong>
								</span>
							</div>
						);
					})}
				</div>
			);
		} else {
			return (<div className="container-fluid"><div className="row"><span className="col-md-12">none</span></div></div>);
		}
    }

}

RootCausePanel.propTypes = {
    rootCause: PropTypes.array,
    loading: PropTypes.bool
};

export default RootCausePanel;
