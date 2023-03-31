# Table of content

<!-- TOC depthFrom:1 depthTo:6 withLinks:1 updateOnSave:1 orderedList:0 -->

- [Table of content](#table-of-content)
- [System Modeller security overview](#system-modeller-security-overview)
	- [Password Authentication](#password-authentication)
		- [Registering default users](#registering-default-users)
	- [JWT tokens](#jwt-tokens)
		- [What does a JWT look like?](#what-does-a-jwt-look-like)
			- [JWT token header](#jwt-token-header)
			- [JWT token payload](#jwt-token-payload)
				- [Public claims](#public-claims)
				- [Private claims](#private-claims)
				- [Example Payload](#example-payload)
		- [JWT token signature](#jwt-token-signature)
		- [JWT Token generation in System Modeller](#jwt-token-generation-in-system-modeller)
	- [System Modeller WebSecurityConfig.java config](#system-modeller-websecurityconfigjava-config)
	- [Default users file](#default-users-file)
	- [JWT package details](#jwt-package-details)
	- [System modeller security workflow](#system-modeller-security-workflow)
		- [Register new user](#register-new-user)
		- [User login](#user-login)
		- [Use of system modeller dashboard/editor](#use-of-system-modeller-dashboardeditor)
- [Useful links](#useful-links)

<!-- /TOC -->

# System Modeller security overview

Current document summarises System Modeller Security setup, configuration, hints as well as overall overview of the security system features/services.

## Password Authentication

Current version of the software contains a classic _username_ and _password_ authentication model. Currently registration to the system modeller is open and anyone can register by specifying a valid username and password (need
to be changed in the future).

**NOTE:** in the future an email should be send to the users in order to complete the registration - the main purpose of it is to validate that users actually have access to that email account and are authorised to use it.

### Registering default users
There is a default list of users that can be registered each time the software will be deployed/redeployed. The file can be easily modified with users being added or deleted. Default user details file is located at:
_src/main/resources/users.json_

There are additional default “_administrator_” and “_guest_” users that will be added to the system modeller by default when the system will be deployed/re-deployed. Details of both default users can be found at the main system modeller configuration file located at:
_src/main/resources/application.properties_.

It is very easy to tell system modeller not to register any users on the system deployment/redeployment and it is done through the _application.properties_ configuration file (mentioned above).

Here are some examples:
- _load.users.from.file_ - if set to true (default) then all users from users.json will be registered system
- _user.path_ - user details file name
- _create.default.admin_ - if set to true (default) then a default “admin” user will be registered to the system
- _create.default.guest_ - if set to true (default) then a default “guest” user will be registered to the system


At the moment ALL users will be deleted from the database on each system deployment/re-deployment cycle and it can be easily configured using the main _application.properties_ configuration file setting:
- _drop.users.collection_ - if set to true (default) then ALL users will be deleted from the database when system modeller will be deployed/re-deployed


## JWT tokens
Before we dive in into specific system modeller JWT token configuration and implementation details it is a good idea to understand what JWT tokens really are!

JSON Web Tokens (JWT), pronounced “jot”, are a standard since the information they carry is transmitted via JSON. We can read more about the draft, but that explanation isn’t the most pretty to look at.

**JSON Web Tokens work across different programming languages:** JWTs work in .NET, Python, Node.js, Java, PHP, Ruby, Go, JavaScript, and Haskell. So you can see that these can be used in many different scenarios.

**JWTs are self-contained:** They will carry all the information necessary within itself. This means that a JWT will be able to transmit basic information about itself, a payload (usually user information), and a signature.

**JWTs can be passed around easily:** Since JWTs are self-contained, they are perfectly used inside an HTTP header when authenticating an API. You can also pass it through the URL.


### What does a JWT look like?
A JWT is easy to identify. It is three strings separated by .

For example:

_aaaaaaaaaa.bbbbbbbbbbb.cccccccccccc_


Since there are 3 parts separated by a ., each section is created differently. We have the 3 parts which are:
- Header
- Payload
- Signature


#### JWT token header
The header carries 2 parts:
- declaring the type, which is JWT
- the hashing algorithm to use (HMAC SHA256 in this case)


Here’s an example:

```
{
  "typ": "JWT",
  "alg": "HS256"
}
```

Now once this is base64encode, we have the first part of our JSON web token:

_eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9_


#### JWT token payload
The payload will carry the bulk of our JWT, also called the JWT Claims. This is where we will put the information that we want to transmit and other information about our token.


There are multiple claims that we can provide. This includes registered claim names, public claim names, and private claim names.
Registered claims
Claims that are not mandatory whose names are reserved for us. These include:
- iss: The issuer of the token
- sub: The subject of the token
- aud: The audience of the token
- exp: This will probably be the registered claim most often used. This will define the expiration in NumericDate value. The expiration MUST be after the current date/time.
- nbf: Defines the time before which the JWT MUST NOT be accepted for processing
- iat: The time the JWT was issued. Can be used to determine the age of the JWT
- jti: Unique identifier for the JWT. Can be used to prevent the JWT from being replayed. This is helpful for a one time use token.

##### Public claims
These are the claims that we create ourselves like user name, information, and other important information.

##### Private claims
A producer and consumer may agree to use claim names that are private. These are subject to collision, so use them with caution.


##### Example Payload
Our example payload has two registered claims (iss, and exp) and two public claims (name, admin).
```
{
  "iss": "scotch.io",
  "exp": 1300819380,
  "name": "Chris Sevilleja",
  "admin": true
}
```

This will encode to:

_eyJpc3MiOiJzY290Y2guaW8iLCJleHAiOjEzMDA4MTkzODAsIm5hbWUiOiJDaHJpcyBTZXZpbGxlamEiLCJhZG1pbiI6dHJ1Z_

That will be the second part of our JSON Web Token.

### JWT token signature
The third and final part of our JSON Web Token is going to be the signature. This signature is made up of a hash of the following components:
- the header
- the payload
- secret

This is how we get the third part of the JWT (example provided using JS):

```
var encodedString = base64UrlEncode(header) + "." + base64UrlEncode(payload);
HMACSHA256(encodedString, 'secret');
```

The secret is the signature held by the server. This is the way that our server will be able to verify existing tokens and sign new ones.


This gives us the final part of our JWT:

_03f329983b86f7d9a9f5fef85305880101d5e302afafa20154d094b229f75773_

Now we have our full JSON Web Token:

_eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzY290Y2guaW8iLCJleHAiOjEzMDA4MTkzODAsIm5hbWUiOiJDaHJpcyBTZXZpbGxlamEiLCJhZG1pbiI6dHJ1ZX0.03f329983b86f7d9a9f5fef85305880101d5e302afafa20154d094b229f75_

### JWT Token generation in System Modeller
Typically when users will want to use protected system modeller APIs they will need to be authenticated to the system which currently can be done by providing their _username/password_ through the login form. On successful authentication a system modeller will start a session for that users and issue a session cookie that users simply use through the system modeller and browser interactions.

The problem arises when system modeller will need to be accessed indirectly or using third party tool (i.e. such as a cmd client) as in some of the cases it would be impossible (or inconvenient) to interact with a standard system modeller login form.
In order to overcome the above there is an important ability for registered users to access protected system modeller APIs without going through standard login procedure (i.e. login HTML page).

JSON Web Token (JWT) is an open standard (RFC 7519) that defines a compact and self-contained way for securely transmitting information between parties as a JSON object. This information can be verified and trusted because it is digitally signed. JWTs can be signed using a secret (with the HMAC algorithm and its perfectly adopted by the system modeller) or a public/private key pair using RSA.

Majority of the JWT token API details are soft coded and are located at application.properties file. Here are the details that will need to be known for a developer in order to configure and use system modeller JWT token API:

- _jwt.route.authentication.path_ - JWT token endpoint name (default value is auth). NOTE: if you will decide to change it make sure that other services will know about it!
- _jwt.route.authentication.refresh_ - JWT token refresh endpoint (is it used at the moment??)
- _jwt.expiration_ - JWT token expiration time in seconds (default value for convenience is set to 31536000 seconds which is equivalent to 1 year)
- _jwt.authorization.cookie.httponly_ - if set to true (default) then JWT token will be transferred back to users only via cookies only for the specified domain (using the HttpOnly flag when generating a cookie helps mitigate the risk of client side script accessing the protected cookie (if the browser supports it))
- _jwt.authorization.cookie.maxage_ - max age for the JWT token cookie (in seconds) where default cookie expiry date is currently set as JWT token expiry date (i.e. 31536000 seconds, which is equivalent to 1 year)
- _jwt.authorization.cookie.secure_ - if set to true (default value is false) then the JWT token cookie will be forced to be used over encrypted channel only (e.g. HTTPS), in production this value should always be set to true
- _jwt.header_ - HTTP request JWT token header (default value is Authorization) that system modeller will use when searching for JWT token value
- _jwt.secret_ - the secret is the signature held by the system modeller, this is the way the system will be able to verify existing tokens and sign new ones


## System Modeller WebSecurityConfig.java config

WebSecurityConfig.java (uk.ac.soton.itinnovation.security.web.WebSecurityConfig.java) is a master class for a
security configuration in system modeller.

In order to fully understand it please follow the comments mapped to the src code below:

```
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.soton.itinnovation.security.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import uk.ac.soton.itinnovation.security.services.ConfigurationService;
import uk.ac.soton.itinnovation.security.services.CustomUserDetailsService;
import uk.ac.soton.itinnovation.security.jwt.JwtAuthenticationEntryPoint;
import uk.ac.soton.itinnovation.security.jwt.JwtAuthenticationTokenFilter;


@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    /**
     * main handler that will redirect a user to the login page in case if user will specify invalid (or expired)
     * JWT token (when accessing protected API)
     */
    @Autowired
    private JwtAuthenticationEntryPoint unauthorizedHandler;

    /**
     * main configuration handler that is in charge of user database manipulations such as:
     * - dropping user database
     * - reading default users from the file and register them to the database
     * - registering default administrator file
     * - register default guest user
     */
    @Autowired
    private ConfigurationService configurationService;

    /**
     * main handler that is able to get details of a persisted storage user (i.e. get/update user details that are stored
     * in the database)
     */
    @Autowired
    CustomUserDetailsService userDetailsService;

    /**
     * configuration setting that will specify what cookies need to be returned to a user after a login
     * note: there is a specific handler for each cookie and new handlers will need to be implemented in case
     * of new additions
     */
    @Value("${trust.builder.cookies}")
    private String[] cookies;

    /**
     * password encoder bean that will is responsible to encoding user passwords during authentication check
     *
     * @return BCryptPasswordEncoder BCryptPasswordEncoder instance
     */
    @Bean()
    public PasswordEncoder configurePasswordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    /**
     * main JWT (JavaScript Web Token) filter that will be monitoring protected APIs and filtering requests that contains Authorization headers (as
     * well as rejecting others if Authorization header will not be specified)
     *
     * @return JwtAuthenticationTokenFilter JwtAuthenticationTokenFilter instance
     * @throws Exception
     */
    @Bean
    public JwtAuthenticationTokenFilter authenticationTokenFilterBean() throws Exception {
        return new JwtAuthenticationTokenFilter();
    }

    /**
     * main handler that will be spawned after a user will successfully authenticate to the system (it is also responsible
     * for cookies generation)
     *
     * @return CustomAuthenticationSuccessHandler CustomAuthenticationSuccessHandler instance
     */
    @Bean
    CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler() {
        return new CustomAuthenticationSuccessHandler();
    }

    /**
     * main handler for successful that will be spawned after a user will successfully logout out of the system (it is also
     * responsible to destroying any previously set by the system cookies)
     *
     * @return
     */
    @Bean
    CustomSimpleUrlLogoutSuccessHandler customSimpleUrlLogoutSuccessHandler() {
        return new CustomSimpleUrlLogoutSuccessHandler();
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        // allow anyone to access this public resources
        web.ignoring()
                .antMatchers("/css/**")
                .antMatchers("/js/**")
                .antMatchers("/data/**")

                // adding images to the public domain as well
                // note: need to be public when users will be accessing models using webkey (requests to get
                // images from the server should not be status for authn/authz checks)
                .antMatchers("/images/**");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                // note: we are not working with csrf tokens
                .csrf().disable()

                // specify earlier declared unauthorized access handler that will redirect users to the login page
                .exceptionHandling().authenticationEntryPoint(unauthorizedHandler).and()

                // don't create session at the moment (note: might need to be revisited in the future)
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()

                // specify that we need to authorize requests for ALL of the endpoints (exclusions will be explicitly applied below)
                .authorizeRequests()

                // permit anyone to access the following public pages
                .antMatchers("/", "/welcome", "/register", "/forgotpassword", "/resetpassword").permitAll()

                // registered users should be able to generate access token (possibly subject to terms and conditions in the future) therefore
                // this endpoint is also open for anyone
                .antMatchers("/auth/**").permitAll()


                // special rule that will allow to access existing models (i.e. edit, validate etc.) if they will know
                // secret model web key tokens
                // note: several endpoints will need to be open in order to skip additional (apart from the secret web key) checks
                // note: instead of using wildcards for majority of access points specify them explicitly
                .antMatchers("/models/*/edit").permitAll()
                .antMatchers("/models/*/").permitAll()
                .antMatchers("/models/*/assets/*").permitAll()
                .antMatchers("/models/*/relations/").permitAll()
                .antMatchers("/models/*/relations/*").permitAll()
                .antMatchers("/models/*/assets/*/controls_and_threats").permitAll()
                .antMatchers("/models/*/assets/*/control").permitAll()
                .antMatchers("/models/*/validated").permitAll()
                .antMatchers("/models/*/screenshot").permitAll()

                // any other requests to ALL api (explisions were explicitly applied above) need to be fully authenticated
                .anyRequest().fullyAuthenticated()

                // login and logout logic
                .and()
                .formLogin()
                .loginPage("/login")
                .successHandler(customAuthenticationSuccessHandler()) // after a successful login custom success handler will be spawned
                .failureUrl("/login?error")
                .permitAll()
                .and()
                .logout()
                .logoutUrl("/logout")
                .logoutSuccessHandler(customSimpleUrlLogoutSuccessHandler()) // after a successful login custom logout handler will be spawned
                .logoutSuccessUrl("/login?logout")
                .permitAll();

        // custom JWT based security filter that will be checking requests to protected APIs (exclusions apply)
        http
            .addFilterBefore(authenticationTokenFilterBean(), UsernamePasswordAuthenticationFilter.class);

        // cache headers
        http.headers().cacheControl();
    }

    /*
    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.inMemoryAuthentication()
                .withUser("gc").password("gc").roles("USER");
        auth.inMemoryAuthentication()
                .withUser("oh").password("oh").roles("USER");
    }
    */

    /**
     * declare master authentication manager builder/handler
     *
     * @param authManagerBuilder
     * @throws Exception
     */
    @Override
    protected void configure(AuthenticationManagerBuilder authManagerBuilder) throws Exception {
        authManagerBuilder.userDetailsService(userDetailsService).passwordEncoder(configurePasswordEncoder());
    }
}
```

## Default users file
There is a file in the system modeller that contains details of default users (by default that are automatically
registered to the system during each system deploy/re-deploy cycle).

The file is named as _users.json_ and its located at _src\main\resources\users.json_.

Here is example snippet of the file:

```
[
    {
        "name": "vk@it-innovation.soton.ac.uk",
        "role": "1",
        "pass": "d5da6404b919ae59f7dfd90d295fb7d2"
    },
    {
        "name": "some_user",
        "role": "2",
        "pass": "acd73eb74cb25ef396e05779bb6a8ddf"
    }
]
```

**NOTE:**: in the above users.json file ALL users are assigned roles (ROLE_USER or ROLE_USER) where **"role": "1"** is equivalent
to ROLE_ADMIN and **"role": "2"** is equivalent to a ROLE_USER.

## JWT package details

JWT package in system modeller (uk.ac.soton.itinnovation.security.jwt) is the main package that is responsible for handling ALL
JavaScript Web Token operations including generation of tokens, their validations as well as various utility checks/functions that
are needed for token manipulations.

Below is a very brief summary of each of the files in that package (note: in order to learn more about each of the class it the best idea
would be to go through each of them and follow javadoc comments).
- _AuthenticationRestController_: main authentication rest controller endpoint that will generate a JWT token (username and password must be specified)
- _AuthorityName_: enum class with two available authorities i.e. ROLE_USER (users) and ROLE_ADMIN (admins)
- _JwtAuthenticationEntryPoint_: authentication entry point (i.e. login page) handler (will be by JWT handlers in case of JWT token will not be specified or
if it will be invalid when trying to access protected APIs)
- _JwtAuthenticationRequest_: JWT authentication request mapping class that will hold details such as username and password
- _JwtAuthenticationResponse_: JWT authentication response mapping class that will hold a JWT token
- _JwtAuthenticationTokenFilter_: main JWT token filter class that will be monitoring Authorization headers, fetching JWT token and making decisions whether
requests should go through (i.e. JWT tokens are valid) or redirect users to the login page (i.e. in case of JWT tokens will be either missing or invalid)
- _JwtTokenUtil_: utility class that will be used by some of the classes in JWT package that contains utility functions such as generating/validation JWT tokens etc.
- _JwtUser_: JWT user mapping class that will hold information of the users during JWT token validation/generation

## System modeller security workflow

Current section summarises how system modeller security services work based on several use case scenarios:
- System modeller is used as a standalone software
- System modeller is embedded to other systems (LCA portal for example)

Prior reviewing the above use case scenarios the are several things that users would need to do:
- Ask system modeller administrators to create a user account for them
- Register their user account on system modeller website (does not apply if system modeller is running in embedded modes)

### Register new user

The easiest way to add a new user to the system would be adding its details to mentioned earlier _users.json_ file (the syntax of adding a new user is self explanatory). The only issue is
the user will not appear on the system until the system modeller will be redeployed.

- **Class responsible for registering default users:** uk.ac.soton.itinnovation.security.services.ConfigurationService

Alternatively use _/register_ endpoint of the system modeller for a quick and easy registration.

- **Class responsible for user registration:** uk.ac.soton.itinnovation.security.web.UserController

**NOTE:** at the moment newly registered user details will be lost after the system will be redeployed and this issue need to be fixed (i.e. backing up user details and restoring them on
each system deploy/re-deploy cycle).

### User login

When using system modeller is used in a standalone mode it will be necessary to authenticate (i.e. login) to the system prior using any restricted APIs (at the moment applicable only to a dashboard where
users can list, create and launch model editor). Registered users should be able to login to the system by specifying their username and password at _/login_ endpoint. After a successful login the UserController
will generate specific set of cookies (cookies containing Authorization JWT token as well as session information) that will be then transfered with each user request (note: in the future Authorization cookie will need to be
replaced by Authorization header!).

 - **Class responsible for user registration:** uk.ac.soton.itinnovation.security.web.UserController

In case if system modeller will be used in embedded mode (e.g. as in LCA portal for example) it will be necessary to generate JWT token in order to create a model (initial step when using system modeller
in embedded mode). JWT token generating is easy and only cost one HTTP POST request to _/auth_ endpoint e.g.:

```
HTTP POST /system-modeller/auth

Headers:
Content-type: application/json

{
	"username": "username",
	"password": "password"
}
```

In case of username and password authentication will be successful after checking a request to _/auth_ endpoint the system modeller will generate response containing JWT token e.g.:

```
{
  "token" : "eyJhb...UxMiJ9.eyJzdWI...wNTc5fQ.ZjP...xrZA"
}

```

JWT token then will be used (as an Authorization header) to create a model by issuing request to _/system-modeller/models/_ endpoint e.g.:

```
HTTP POST /system-modeller/models/

Headers:
Content-type: application/json
Authorization: eyJhb...UxMiJ9.eyJzdWI...wNTc5fQ.ZjP...xrZA

{
	"name":"test model 1",
	"ontology": "ASSURED"
}
```

If JWT token validation be successful then a new model (with specified name) will be create e.g.:

```
{
  "id" : "58347c357971dd4412387d34",
  "name" : "test model 1",
  "description" : null,
  "ontology" : "ASSURED",
  "valid" : false,
  "user" : "vk@it-innovation.soton.ac.uk",
  "created" : 1479834677401,
  "modified" : 1479834677401
}
```

The above request will contain a **secret hard-to-guess token** (i.e. model id) that will be used to securely access a model. In this case the secret hard-to-guess token is _58347c357971dd4412387d34_
and its corresponding model will be accessible using the following URI endpoint _/system-modeller/models//58347c357971dd4412387d34/edit_.

**Classes responsible to ALL JWT token manipulations (i.e. generation, validating etc.)**:
- uk.ac.soton.itinnovation.security.jwt.AuthenticationRestController: main authentication rest controller endpoint that will generate a JWT token (username and password must be specified)
- uk.ac.soton.itinnovation.security.jwt.AuthorityName: enum class with two available authorities i.e. ROLE_USER (users) and ROLE_ADMIN (admins)
- uk.ac.soton.itinnovation.security.jwt.JwtAuthenticationEntryPoint: authentication entry point (i.e. login page) handler (will be by JWT handlers in case of JWT token will not be specified or
if it will be invalid when trying to access protected APIs)
- uk.ac.soton.itinnovation.security.jwt.JwtAuthenticationRequest: JWT authentication request mapping class that will hold details such as username and password
- uk.ac.soton.itinnovation.security.jwt.JwtAuthenticationResponse: JWT authentication response mapping class that will hold a JWT token
- uk.ac.soton.itinnovation.security.jwt.JwtAuthenticationTokenFilter: main JWT token filter class that will be monitoring Authorization headers, fetching JWT token and making decisions whether
requests should go through (i.e. JWT tokens are valid) or redirect users to the login page (i.e. in case of JWT tokens will be either missing or invalid)
- uk.ac.soton.itinnovation.security.jwt.JwtTokenUtil: utility class that will be used by some of the classes in JWT package that contains utility functions such as generating/validation JWT tokens etc.
- uk.ac.soton.itinnovation.security.jwt.JwtUser: JWT user mapping class that will hold information of the users during JWT token validation/generation

### Use of system modeller dashboard/editor

Model editor can be accessible by anyone (whether system modeller is running in standalone or embedded modes) if secret hard-to-guess token (i.e. model id) is known. At this point (i.e. if
secret hard-to-guess token will be known) Authorization JWT token will not be needed and all model operations are permitted (such as edit, validate etc.).

The above was possible to achieve by opening specific model endpoints in the WebSecurityConfig class e.g.:

```
// special rule that will allow to access existing models (i.e. edit, validate etc.) if they will know
// secret model web key tokens
// note: several endpoints will need to be open in order to skip additional (apart from the secret web key) checks
// note: instead of using wildcards for majority of access points specify them explicitly
.antMatchers("/models/*/edit").permitAll()
.antMatchers("/models/*/").permitAll()
.antMatchers("/models/*/assets/*").permitAll()
.antMatchers("/models/*/relations/").permitAll()
.antMatchers("/models/*/relations/*").permitAll()
.antMatchers("/models/*/assets/*/controls_and_threats").permitAll()
.antMatchers("/models/*/assets/*/control").permitAll()
.antMatchers("/models/*/validated").permitAll()
.antMatchers("/models/*/screenshot").permitAll()
```

- **Class responsible for model manipulations (if secret hard-to-guess token is known):** uk.ac.soton.itinnovation.security.web.WebSecurityConfig

# Useful links
- Using JSON web tokens as API keys - https://auth0.com/blog/using-json-web-tokens-as-api-keys/  
- Detailed information about JWT tokens - https://jwt.io/
