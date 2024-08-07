# Licenses in the Spyderisk System Modeller and how to apply them

This is both a policy document and a practical how-to. The technical details of
licensing can be complicated, but Spyderisk licensing is easy if you follow
these basic rules. If you have any questions do please ask the
[spyderisk-dev mailing list](mailto://spyderisk-dev@jiscmail.ac.uk),
which has
[archives online](https://www.jiscmail.ac.uk/cgi-bin/wa-jisc.exe?A0=SPYDERISK-DEV).

Licenses apply to all intellectual property in the Spyderisk project.
We apply licenses in the ways specified in
the [REUSE](https://reuse.software/spec/) specification of files and directories. Within
individual source files, according to the 
[SPDX software component Bill of Materials](https://spdx.dev/) specification. For code
that we create, we choose the license. For third-party code, we use whatever license 
was chosen for that code (assuming it is compatible with Spyderisk at all - otherwise
we couldn't use that third-party code!)

There are four types of intellectual property created by Spyderisk project members
specifically for including in Spyderisk:

* software source code
* documentation, including images and other media
* academic papers and reports
* configuration files and examples, which we choose to regard the same as documentation

A fifth type of intellectual property is that created by external third-party
contributors who have probably never even heard of Spyderisk, of any of the
above four types. We already use a lot of such code to avoid re-inventing existing
functionality, all of which is compatible with our licensing policy and some of which 
is not under the Apache license.

We currently use these licenses in Spyderisk:

* *[Apache 2](./APACHE-2.0.txt)* for nearly all code, including all code created specifically for Spyderisk
* *[Creative Commons By-SA 4.0](./CREATIVE-COMMONS-BY-SA-4.0.txt)* for all new documentation, and eventually all documentation will be copyright CC By SA unless it was created by someone 
* *[MIT](./MIT.txt)* Some third party front-end elements (including the Bootstrap and JQuery libraries)
* *[ISC](./ISC.txt)* and *[BSD 3-Clause](./BSD-3-CLAUSE.txt)* for some other third-party code

As you can see, Spyderisk is happy to consider any useful third-party code or
documentation for inclusion in Spyderisk provided it is under a compatible
license. There is occasionally some nuance to what "compatible license" means,
as described below, but this is our general intention.

# Apache 2.0 license - default for source code

In some cases other licenses may be used if the code originated from a third party.
So long as the third party code has a license compatible with the
[Open Source Definition](https://opensource.org/osd/) then it will not conflict with
the Apache 2.0 license and we can freely use it.

In order to apply the Apache license to a source code file in the Spyderisk
project, insert the following comment block at the top, replacing the text in
[square brackets] with the correct values.

```
Copyright [YEAR] The Spyderisk Licensors

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at:

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

<!-- SPDX-License-Identifier: Apache 2.0 -->
<!-- SPDX-FileCopyrightText: [YEAR] The Spyderisk Licensors -->
<!-- SPDX-ArtifactOfProjectName: Spyderisk -->
<!-- SPDX-FileType: Source code -->
<!-- SPDX-FileComment: Original by [NAME OF CONTRIBUTOR], [MONTH] [YEAR] -->
```

# Creative Commons BY-SA - documentation and config files

We have decided not to apply copyright headers to README files such as the one you are reading, because
the REUSE standard already brands every file, and it would just be messy and distracting.
Similarly we do not add copyright headers to images, we just make a statement in a file 
covering all the images. However most non-Markdown forms of documentation do have explicit CC BY-SA
license at the top.

```
Copyright 2023 The Spyderisk Authors

<!-- SPDX-License-Identifier: CC-BY-SA-4.0 -->
<!-- SPDX-FileCopyrightText: 2023 The Spyderisk Authors -->
<!-- SPDX-ArtifactOfProjectName: Spyderisk -->
<!-- SPDX-FileType: Documentation -->
<!-- SPDX-FileComment: Original by Dan Shearer, October 2023 -->
```

# What about third-party GPL code?

No.

We cannot use GPLv2 licensed code because it is the one major open source license which is
[incompatible with the Apache license](https://en.wikipedia.org/wiki/Apache_License#Compatibility).
Spyderisk uses the JSPlumb library, which is dual-licensed under MIT and GPLv2, and we choose to 
use it under the MIT license so there is no conflict.

... and also *Maybe*, perhaps.

Unlike version 2, the [GPLv3](https://www.gnu.org/licenses/gpl-3.0.txt) is
compatible with Apache 2.0, **but only in one direction**.  After codebases
under these two licenses are combined, the combined result can only be
distributed under the GPLv3 (again, there are some additional but this is
approximately correct.) In the theoretical case where the Spyderisk Project
decided to mix its code with a significant piece of third party software which
is only available under the GPLv3, AGPLv3 or LGPLv3, then the whole of
Spyderisk (when compiled) could only be distributed under the GPLv3 terms.

That is certainly possible, but it would be a big change and not what was
intended at the time Spyderisk was placed under Open Source licences by our
generous founding donor, the University of Southampton. In that unexpected case,
we might even consider re-licensing to GPLv3.

Until that time, we will review very carefully any proposed imports of GPLv3 code
into the tree. We're not saying "no", but we will be cautious.
