# Contributing to the Spyderisk System Modeller Project

Welcome!

We'd love to have your contributions to the Spyderisk System Modeller project. This document is
about our practical principles of working. 

The overall Spyderisk aim is to

> understand the trustworthiness of socio-technical systems by establishing an international Open Community supporting the research, development, use and support of open, effective, and accessible risk assessment methods, knowledge and tools.

and you can read more about this in the [general Spyderisk description](https://github.com/Spyderisk/), which explains
who we are and what we do.

Please read our [Code of Conduct](../CODE-OF-CONDUCT.md) to keep our community approachable and
respectful.

# Who can contribute?

We need all the help we can get on the software and computing side
of Spyderisk such as Java, python and web development, system configuration,
software packaging, build/test etc. There is lots of computer science in Spyderisk.

That said, some of the most important work is not by computer scientists.

We also need help from:

* modellers (creating descriptions of real-world situations in a form that Spyderisk can operate on)
* documenters (describing the current state of risk assessment knowledge, and how Spyderisk implements this)
* risk specialists (how can we decide what is important? how do we correctly calibrate our response?)
* ontologists (conceptual understanding of societal goods, risks, threats, harms, attacks, vulnerabilities etc)
* mathematicians (risk modelling methodologies, robustness of calculations etc)
* legal specialists (EU legislation on Cyber Resilience, AI, Medical Devices etc)
* graphic designers (have you seen the corners on our icons??)

If you are any of the above, including a coder, we would love to hear from you.
Do please drop an email to [team@spyderisk.org](mailto://team@spyderisk.org)
or open a discussion issue on GitHub.

# Getting started for software developers

* The [system modeller README](../README.md) explains how to set up the development environment
* Once you have a working local copy of Spyderisk you can run the demonstration System models to get a feel for things
* It is likely that while doing the above you will already have noticed things that need to be fixed. Great! This document shows you how to make these fixes happen, or
* Alternatively, you can find an issue from our [List of Open Issues](https://github.com/Spyderisk/system-modeller/issues) you think you would like to solve, and add a comment to say that you are working on a fix, or
* Create a new query or bug report as described in the following section, and start working on a fix for it
* Whatever you decide to work on, follow the "How to submit a patch" procedure below

# How to open a query or bug report

At this stage in our young open project, two things are true: there are many bugs to find, and, very often a problem is because the user does not understand how Spyderisk works. If its the latter, then you have not found a bug. When you have a problem, we recommend you:

* Open a [new issue in system-modeller](https://github.com/Spyderisk/system-modeller/issues/new)
* Select the template marked "New Spyderisk query". If you are very sure its a bug, select "New Spyderisk bug report"

# How to submit a patch

You are about to make us very happy. There are several cases:

* Documentation fix - [create a fork and send a pull request](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/proposing-changes-to-your-work-with-pull-requests/creating-a-pull-request-from-a-fork)
* Obvious code fix - create a fork and pull request, just as for documentation
* Any other code fix - please create a query or bug report according to the previous section. It may well be that you have code which is good to go, but in our young open project there is a lot of context that might be relevant to what you wish to do.

But basically just talk to us using whatever means you are comfortable with, and we will figure it out.

# Spyderisk project principles

## Openness first

* Our software licensing is Apache 2, and analogously for documentation
* Our communication is collaborative and collective
* We build our software around openly published academic theory

## Version control is mandatory

* Our software is under public version control.
* Our models expressed in data dumps are also under version control
* We create [PURL permanent URLs for software and documentation](https://purl.archive.org/domain/spyderisk) when there are important new versions. PURL is maintained by [archive.org](https://archive.org) which we hope is stable for the long term
* We have some legacy software outside the system-modeller Git tree which cannot yet be versioned, but we are working hard on that

## Transparency trumps accuracy

Spyderisk needs to be both trustable and also to progress quickly. Where there
is incomplete or inaccurate work in the Spyderisk System Modeller code then we document
this with the string:

```
WIP: BRIEF TEXT DESCRIPTION, https://github.com/Spyderisk/system-modeller/issues/NNN
```

Where "BRIEF TEXT DESCRIPTION" should not exceed a couple of sentences, and NNN
should be the most relevant issue.


# Communication with the Spyderisk community

* tbd

# Wiki and documentation

* tbd
