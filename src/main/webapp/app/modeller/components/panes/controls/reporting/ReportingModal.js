import React from "react";
import PropTypes from "prop-types";
import axios from "axios";
import {
    Alert,
    Button,
    FormControl,
    FormGroup,
    ControlLabel,
    Modal,
} from "react-bootstrap";

const REPORTS = {
    "security-27001": {
        label: "Cybersecurity - ISO 27001",
        isoStandard: "27001",
        reportType: "security",
        filename: "cybersecurity-27001-report.pdf",
    },
    "security-14971": {
        label: "Cybersecurity - ISO 14971",
        isoStandard: "14971",
        reportType: "security",
        filename: "cybersecurity-14971-report.pdf",
    },
    compliance: {
        label: "Compliance",
        isoStandard: "27001",
        reportType: "compliance",
        filename: "compliance-report.pdf",
    },
    "combined-27001": {
        label: "Combined - ISO 27001 and compliance",
        isoStandard: "27001",
        reportType: "combined",
        filename: "combined-27001-compliance-report.pdf",
    },
    "combined-14971": {
        label: "Combined - ISO 14971 and compliance",
        isoStandard: "14971",
        reportType: "combined",
        filename: "combined-14971-compliance-report.pdf",
    },
};

const delay = (milliseconds) => new Promise((resolve) => setTimeout(resolve, milliseconds));

class ReportingModal extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            report: "security-27001",
            generating: false,
            error: null,
            elapsedSeconds: 0,
        };
        this.generateReport = this.generateReport.bind(this);
        this.handleHide = this.handleHide.bind(this);
    }

    handleHide() {
        if (!this.state.generating) {
            this.props.onHide();
        }
    }

    async generateReport() {
        const report = REPORTS[this.state.report];
        this.setState({generating: true, error: null, elapsedSeconds: 0});
        const startedAt = Date.now();

        try {
            const authzResponse = await axios.get(
                process.env.config.API_END_POINT + "/models/" + this.props.modelId + "/authz",
                {headers: {"X-Requested-With": "XMLHttpRequest"}}
            );
            const readUrl = authzResponse.data && authzResponse.data.readUrl;
            if (!readUrl) {
                throw new Error("The model read-only URL is unavailable");
            }

            const targetUrl = window.location.origin
                + process.env.config.API_END_POINT
                + "/models/" + readUrl + "/read";
            const createResponse = await axios.post(
                process.env.config.API_END_POINT
                    + "/adaptor/api/v2/ssmtools/reporting/create-report-from-url-async",
                null,
                {
                    params: {
                        target_url: targetUrl,
                        iso_standard: report.isoStandard,
                        report_type: report.reportType,
                        output_format: "pdf",
                    },
                    headers: {"X-Requested-With": "XMLHttpRequest"},
                }
            );
            const reportId = createResponse.data && createResponse.data.rjob_id;
            if (!reportId) {
                throw new Error("The reporting job was not created");
            }

            let jobStatus = createResponse.data.status;
            while (jobStatus !== "finished") {
                if (jobStatus === "failed") {
                    throw new Error("The reporting job failed");
                }
                await delay(2000);
                const statusResponse = await axios.get(
                    process.env.config.API_END_POINT
                        + "/adaptor/api/v2/ssmtools/reporting/status/" + reportId,
                    {headers: {"X-Requested-With": "XMLHttpRequest"}}
                );
                jobStatus = statusResponse.data && statusResponse.data.status;
                this.setState({elapsedSeconds: Math.floor((Date.now() - startedAt) / 1000)});
            }

            const response = await axios.get(
                process.env.config.API_END_POINT
                    + "/adaptor/api/v2/ssmtools/reporting/download/" + reportId,
                {
                    responseType: "blob",
                    headers: {"X-Requested-With": "XMLHttpRequest"},
                }
            );

            const objectUrl = window.URL.createObjectURL(
                new Blob([response.data], {type: "application/pdf"})
            );
            const link = document.createElement("a");
            link.href = objectUrl;
            link.download = report.filename;
            document.body.appendChild(link);
            link.click();
            link.remove();
            window.URL.revokeObjectURL(objectUrl);
            this.setState({generating: false, elapsedSeconds: 0});
        } catch (error) {
            console.error("Report generation failed", error);
            this.setState({
                generating: false,
                elapsedSeconds: 0,
                error: "Reporting error. This may be because the report was requested by someone other than the model owner.",
            });
        }
    }

    render() {
        return (
            <Modal show={this.props.show} onHide={this.handleHide} backdrop="static">
                <Modal.Header closeButton>
                    <Modal.Title>Generate Report</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    {this.state.error && (
                        <Alert bsStyle="danger">{this.state.error}</Alert>
                    )}
                    {this.state.generating && (
                        <Alert bsStyle="info">
                            Generating report. Large models may take several minutes.
                            {" "}Elapsed time: {this.state.elapsedSeconds} seconds.
                        </Alert>
                    )}
                    <FormGroup controlId="report-type">
                        <ControlLabel>Report</ControlLabel>
                        <FormControl
                            componentClass="select"
                            value={this.state.report}
                            disabled={this.state.generating}
                            onChange={(event) => this.setState({report: event.target.value, error: null})}
                        >
                            {Object.keys(REPORTS).map((key) => (
                                <option key={key} value={key}>{REPORTS[key].label}</option>
                            ))}
                        </FormControl>
                    </FormGroup>
                </Modal.Body>
                <Modal.Footer>
                    <Button onClick={this.props.onHide} disabled={this.state.generating}>
                        Cancel
                    </Button>
                    <Button
                        bsStyle="primary"
                        onClick={this.generateReport}
                        disabled={this.state.generating}
                    >
                        {this.state.generating ? (
                            <span><i className="fa fa-refresh fa-spin" /> Generating...</span>
                        ) : (
                            <span><i className="fa fa-file-pdf-o" /> Generate PDF</span>
                        )}
                    </Button>
                </Modal.Footer>
            </Modal>
        );
    }
}

ReportingModal.propTypes = {
    modelId: PropTypes.string.isRequired,
    show: PropTypes.bool,
    onHide: PropTypes.func.isRequired,
};

export default ReportingModal;
