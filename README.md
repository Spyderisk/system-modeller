# The Spyderisk System Modeller software

This is the main software part of the [Spyderisk Open Project](https://github.com/Spyderisk).

The overall Spyderisk Project's aim is to:

> understand the trustworthiness of socio-technical systems by establishing an international Open Community supporting the research, development, use and support of open, effective, and accessible risk assessment methods, knowledge and tools.

The Spyderisk System Modeller software ("Spyderisk") provides a thorough
risk assessment of complex systems, applying our mathematical modelling to your
particular problem. This software is the result of 
[15 years of history in risk assessment](https://github.com/Spyderisk/system-modeller/blob/dev/HISTORY.md)
of complex socio-technical systems. Starting in 2023 we publish everything under
open licenses: software source code, ontologies, domain model database,
online training, documentation, and academic papers. 

As of Mid-2024, the Spyderisk software is in early release, fully available but only working
in quite specific circumstances. This README file signposts you to the
different ways of trying out Spyderisk and learning about the theory behind it.

If you are a researcher in the area of risk modelling including ontologies of
risk, or if you have a specific problem domain you need to solve (particularly
in cybersecurity or privacy) then Spyderisk could be for you. 

# Contents

* [Introduction](#introduction)
* [Important project information](#important-project-information)
* [What is the Spyderisk System Modeller?](#what-is-the-spyderisk-system-modeller)
* [Your next steps](#your-next-steps)
* [Process of using system-modeller](#process-of-using-system-modeller)


# Introduction

This README relates to the [system-modeller source tree](https://github.com/Spyderisk/system-modeller),
which provides both a web service and a web-based user interface. 
If you only wish to install and run Spyderisk and its
web GUI, see instead the [Spyderisk Deployment Project](https://github.com/Spyderisk/system-modeller-deployment).
Spyderisk will only build and run on Linux, however, it can be deployed to non-Linux systems
using Docker containers. Docker is used to provide a consistent build and test environment for
developers and for the continuous integration (CI) system. 

This source tree is for:

* those who want to inspect or change the [Spyderisk source code](./src/main/java/uk/ac/soton/itinnovation/security/README.md)
* building and running Spyderisk from its source code
* reading all [Spyderisk technical papers](./docs/papers/README.md) in one place
* understanding the generous [Spyderisk open licensing](./LICENSES.md)
* reading the [schema explanation](./docs/triple-store-schema.md) for the [base ontology in RDF format](https://github.com/Spyderisk/system-modeller/blob/dev/src/main/resources/core.rdf) which ships with Spyderisk

If you wish to interact programmatically with Spyderisk instead of using the
web GUI, the [Spyderisk Python adaptor](https://github.com/Spyderisk/system-modeller-adaptor)
may be for you. This is the way you can call the Spyderisk web service API to create, update, analyse and query
system models and integrate other tools. While this is also Spyderisk software development, it
is much higher-level than the source code of the Spyderisk application found in this tree. The
Java application creates the reasoner service which the Python adapter can interrogate.

# Important project information

Spyderisk is created by the [Spyderisk Contributors](./CONTRIBUTORS.md), freely
available under [Open Source terms](./LICENSE.md). Everyone is welcome, noting
our [basic rules of decent behaviour](./CODE-OF-CONDUCT.md) around Spyderisk,
which includes contact details if you want to report a behaviour problem.

We try to make it easy to [contribute to Spyderisk](./CONTRIBUTING.md) whatever your skills.

You can contact us by:
* [raising a GitHub Issue](https://github.com/Spyderisk/system-modeller/issues/new)
* emailing [team@spyderisk.org](mailto://team@spyderisk.org)

# What is the Spyderisk System Modeller?

The Spyderisk System Modeller is a generic risk assessment tool.  Spyderisk
must be supplied with a model of a domain of study, which defines a 
simplified version of the real world and the different threats and mitigations
that apply. The use case we have developed the most relates to cybersecurity
analysis, however the Spyderisk team also models risks in other areas including
medical devices and privacy. We call a domain model the "knowledgebase", and it
uses ontological methods.

Spyderisk does not come bundled with any particular knowledgebase; this is
configurable at build/deploy time, by putting one or more zip bundles into the
"knowledgebases" folder (described below). We publish and
maintain our most advanced
[knowledgebase for complex networked systems](https://github.com/Spyderisk/domain-network/packages/1826148)
in its own GitHub repository.

When using our knowledgebase for cybersecurity analysis, Spyderisk assists the user in following
the risk assessment process defined in ISO 27005 from the
[ISO 27001](https://en.wikipedia.org/wiki/ISO/IEC_27000-series)
of standards. We found the 27k standards do not have all the required concepts 
for effective risk modelling, and our knowledgebase is significantly richer than 
what is found in the standards. Since a Spyderisk knowledgebase is based on an underlying
ontology, we have created an ontology which is broadly compatible with the ISO27k terminology.

The system-modeller tree has approximately 70k lines of Java code in the core service,
and another 20k of Java code for running tests.


# Your next steps

If you only want to run a demo of the Spyderisk System Modeller and do not need to do any development,
then you need to follow the [Installing Docker](./INSTALL.md#installing-docker) section of [INSTALL.md](./INSTALL.md)
and then use the [Spyderisk Deployment project](https://github.com/Spyderisk/system-modeller-deployment).
We also [have two comprehensive online training courses](https://training.spyderisk.org/courses/), covering
both Risk Assessment and Using Spyderisk.

From here, within this source tree, you may:

* [compile and install Spyderisk from source code](./INSTALL.md)
* [start Spyderisk software development](./docs/development.md)

# Process of using system-modeller

Once installed, whether from the source code in this software tree or via 
the Spyderisk deployment project, the graphical web user interface guides the
user through the following steps:

1. The user draws a model of their system model by dragging and dropping typed
   assets linked by typed relations onto a canvas.
2. The software analyses the model, inferring network paths, data flows,
   client-service trust relationships and much more (depending on the
knowledgebase).
3. The software analyses the model to find all the threats and potential
   controls that are encoded in the knowledgebase. The threats are
automatically chained together via their consequences to create long-reaching
and inter-linked attack graphs and secondary threat cascades through the
system.
4. The user assigns impact levels to various failure modes on the primary
   assets only.
5. The user can add controls to the model to reduce the likelihood of threats.
6. The software does a risk analysis, considering the external environment, the
   defined impact levels, the controls, and the chains of threats that have
been discovered. The threats and consequences may then be ranked by their risk,
highlighting the most important problems.
7. The user can choose to add or change the controls (back to step 5), to
   redesign the system (step 1), or to accept the system design.
8. The software can output reports describing the system along with the
   threats, consequences and their risk levels.

The knowledgebase describes threats through patterns of multiple assets along
with their context (such as network or physical location), rather than assuming
that threats relate to a single asset type. Similarly, methods to reduce threat
likelihood ("control strategies") may comprise multiple controls on different
assets (for example, both an X509 certificate at a service and verification of
the certificate at the client). Knowledgebases may also be designed such that
control strategies help solve one problem but exacerbate another (for example,
adding a password reduces the likelihood of unauthorised access to a service
but increases the likelihood of the legitimate user failing to get in). All
this provides a high degree of realism to the analysis.

With a compatible knowledgebase, the software can perform a both long-term risk
assessment suitable for when designing a system, and an operational (or
"runtime") risk assessment using a short time horizon. Different controls are
appropriate in each case (for instance, implementing a new staff security
training policy does not help with an ongoing attack, but blocking a network
path does). For the operational risk assessment, the state of the system model
must first be synchronised with the current operational state (for instance
through integration via the API with OpenVAS or a SIEM).


