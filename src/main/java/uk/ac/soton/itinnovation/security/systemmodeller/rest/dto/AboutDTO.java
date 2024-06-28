/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2024
//
// Copyright in this software belongs to University of Southampton
// IT Innovation Centre of Gamma House, Enterprise Road,
// Chilworth Science Park, Southampton, SO16 7NS, UK.
//
// This software may not be used, sold, licensed, transferred, copied
// or reproduced in whole or in part in any manner or form or in or
// on any media by any person other than in accordance with the terms
// of the Licence Agreement supplied with the software, or otherwise
// without the prior written consent of the copyright owners.
//
// This software is distributed WITHOUT ANY WARRANTY, without even the
// implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
// PURPOSE, except where stated in the Licence Agreement supplied with
// the software.
//
//      Created By :            Ken Meacham
//      Created Date :          24/06/2024
//      Created for Project :   Cyberkit4SME
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.rest.dto;

public class AboutDTO {
    private String spyderiskVersion;
    private String spyderiskCommitSha;
    private String spyderiskCommitTimestamp;
    private String spyderiskAdaptorVersion;

    private AboutLinkDTO website;
    private AboutLinkDTO license;
    private AboutLinkDTO contributors;

    public String getSpyderiskVersion() {
        return spyderiskVersion;
    }

    public void setSpyderiskVersion(String spyderiskVersion) {
        this.spyderiskVersion = spyderiskVersion;
    }

    public String getSpyderiskCommitSha() {
        return spyderiskCommitSha;
    }

    public void setSpyderiskCommitSha(String spyderiskCommitSha) {
        this.spyderiskCommitSha = spyderiskCommitSha;
    }

    public String getSpyderiskCommitTimestamp() {
        return spyderiskCommitTimestamp;
    }

    public void setSpyderiskCommitTimestamp(String spyderiskCommitTimestamp) {
        this.spyderiskCommitTimestamp = spyderiskCommitTimestamp;
    }

    public String getSpyderiskAdaptorVersion() {
        return spyderiskAdaptorVersion;
    }

    public void setSpyderiskAdaptorVersion(String spyderiskAdaptorVersion) {
        this.spyderiskAdaptorVersion = spyderiskAdaptorVersion;
    }

    public AboutLinkDTO getWebsite() {
        return website;
    }

    public void setWebsite(AboutLinkDTO website) {
        this.website = website;
    }

    public AboutLinkDTO getLicense() {
        return license;
    }

    public void setLicense(AboutLinkDTO license) {
        this.license = license;
    }

    public AboutLinkDTO getContributors() {
        return contributors;
    }

    public void setContributors(AboutLinkDTO contributors) {
        this.contributors = contributors;
    }

}