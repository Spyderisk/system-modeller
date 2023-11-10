# Licenses in Spyderisk and how to apply them

We apply licenses to all intellectual property in the Spyderisk project, unless
it already comes from some other authors and already has a license.

There are four types of intellectual property in Spyderisk:

* software source code
* documentation, including images and other media
* configuration files and examples, which we regard the same as documentation
* third-party contributions for each of the above three types

We currently use these licenses in Spyderisk:

* *Apache 2* nearly all code.
* *Creative Commons By-SA 4.0* Eventually all documentation will be copyright CC By SA.
* *MIT* A few third party front-end elements including the Bootstrap and JQuery libraries)

We are happy to consider any useful third-party code or documentation for inclusion in Spyderisk
provided it is under a compatible license. There is occasionally some nuance to
what "compatible license" means, as described below, but this is our general outlook.

# Apache 2.0 license - default for source code

In some cases other licenses may be used if the code originated from a third party.
So long as the third party code has a license compatible with the
[Open Source Definition](https://opensource.org/osd/) then it will not conflict with
the Apache 2.0 license and we can freely use it.

https://www.apache.org/licenses/LICENSE-2.0.txt

In order to apply the Apache license to a source code file in the Spyderisk
project, insert this at the top within an appropriate comment block for the language
you are using, replacing the text in [brackets] with the correct values.

```
Copyright [YEAR] The Spyderisk Authors

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
<!-- SPDX-FileCopyrightText: [YEAR] The Spyderisk Authors -->
<!-- SPDX-ArtifactOfProjectName: Spyderisk -->
<!-- SPDX-FileType: Source code -->
<!-- SPDX-FileComment: Original by [NAME OF AUTHOR], [MONTH] [YEAR] -->
```

# Creative Commons BY-SA - documentation and config files

We have decided not to apply copyright headers to README files such as this one, because
we do not actually have to brand every file. We do not, for example, add copyright headers
to images, we just make a statement in a file about all the images. However most text forms
of documentation do have explicit CC BY-SA license at the top.

https://creativecommons.org/licenses/by-sa/4.0/deed.en

```
Copyright 2023 The Spyderisk Authors

<!-- SPDX-License-Identifier: CC-BY-SA-4.0 -->
<!-- SPDX-FileCopyrightText: 2023 The Spyderisk Authors -->
<!-- SPDX-ArtifactOfProjectName: Spyderisk -->
<!-- SPDX-FileType: Documentation -->
<!-- SPDX-FileComment: Original by [NAME OF AUTHOR], [MONTH] [YEAR] -->
```

# What about third-party GPL code?

No.

We cannot use GPLv2 licensed code because it is the one major open source license which is
[incompatible with the Apache license](https://en.wikipedia.org/wiki/Apache_License#Compatibility).
Spyderisk uses the JSPlumb library, which is dual-licensed under MIT and GPLv2, and we choose to 
use it under the MIT license so there is no conflict.

... and Maybe.

The GPLv3 is compatible with Apache 2.0, but only in one direction. After the
two codebases are combined, the result can only be distributed under the GPL
(again, there is some nuance but this is approximately correct.) In the
theoretical case of a significant piece of third party software only being
available under the GPLv3, AGPLv3 or LGPLv3, then the whole of Spyderisk would
be distributed under the GPL terms.

That is certainly possible, but it would be a big change and not what was
intended at the time Spyderisk was placed under Open Source licences by our
generous founding donor, the University of Southampton. In that unexpected case,
we might even consider re-licensing to GPLv3.

Until that time, we will review very carefully any proposed imports of GPL code
into the tree. We're not saying "no", but we will be cautious.
