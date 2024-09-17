/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2016
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
//      Created By :          ?
//      Created Date :        ?
//      Modified by:          Oliver Hayes
//      Created for Project : 5G-ENSURE
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.auth;

import org.keycloak.adapters.springsecurity.KeycloakConfiguration;
import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationProvider;
import org.keycloak.adapters.springsecurity.config.KeycloakWebSecurityConfigurerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;

@KeycloakConfiguration
@EnableWebSecurity
public class WebSecurityConfig extends KeycloakWebSecurityConfigurerAdapter {

	private static final Logger logger = LoggerFactory.getLogger(WebSecurityConfig.class);

	@Value("${admin-role}")
	public String adminRole;

	@Value("${user-role}")
	public String userRole;

	/**
	 * Defines the session authentication strategy.
	 */
	@Bean
	@Override
	protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
		return new RegisterSessionAuthenticationStrategy(new SessionRegistryImpl());
	}

	/**
	 * Registers the KeycloakAuthenticationProvider with the authentication manager.
	 * Authority mapper is needed so that roles don't have to be prepended with ROLE_
	 */
	@Autowired
	public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
		KeycloakAuthenticationProvider keycloakAuthenticationProvider = keycloakAuthenticationProvider();
		keycloakAuthenticationProvider.setGrantedAuthoritiesMapper(new SimpleAuthorityMapper());
		auth.authenticationProvider(keycloakAuthenticationProvider);
	}

	@Override
	public void configure(WebSecurity web) throws Exception {
		// allow anyone to access this public resources
		web.ignoring()
			.antMatchers("/css/**")
			.antMatchers("/js/**")
			.antMatchers("/data/**")
			.antMatchers("/dist/**")

			// adding images to the public domain as well
			// note: need to be public when users will be accessing models using webkey (requests to get
			// images from the server should not be status for authn/authz checks)
			.antMatchers("/images/**");
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		super.configure(http);

		http
			// jh17: by default, csrf tokens break a lot of the application
			// therefore it should be enabled but ignore pretty much everything apart from login/logout forms
			//.csrf().ignoringAntMatchers("/models", "/models/**", "/domains/**", "/domain-manager/**", "/admin/").and()
			//FOR NOW WE WILL KEEP IT DISABLED AS IT BREAKS A LOT OF OTHER THINGS
			.csrf().disable()

			// specify that we need to authorize requests for ALL of the endpoints (exclusions will be explicitly applied below)
			.authorizeRequests()

			// permit anyone to access the following public pages
			.antMatchers("/", "/welcome").permitAll()
			.mvcMatchers(HttpMethod.GET, "/about").permitAll()

			// Explicitly list all operations on models that can be performed unauthenticated.
			// We need to explicitly restrict "/models" as "/models/**" also matches it.
			.mvcMatchers("/models").hasRole(userRole)
			.mvcMatchers(HttpMethod.DELETE, "/models/**/assets/**").permitAll()
			.mvcMatchers(HttpMethod.DELETE, "/models/**/relations/**").permitAll()
			.mvcMatchers(HttpMethod.DELETE, "/models/**").permitAll()
			.mvcMatchers(HttpMethod.POST, "/models/**").permitAll()
			.mvcMatchers(HttpMethod.PATCH, "/models/**/assets/**").permitAll()
			.mvcMatchers(HttpMethod.POST, "/models/**/assets").permitAll()
			.mvcMatchers(HttpMethod.POST, "/models/**/relations").permitAll()
			.mvcMatchers(HttpMethod.POST, "/models/**/threats/**/accept").permitAll()
			.mvcMatchers(HttpMethod.POST, "/models/**/authz").permitAll()
			.mvcMatchers(HttpMethod.PUT, "/models/**/assets/**").permitAll()
			.mvcMatchers(HttpMethod.PUT, "/models/**/relations/**").permitAll()
			.mvcMatchers(HttpMethod.PUT, "/models/**/misbehaviours/**").permitAll()
			.mvcMatchers(HttpMethod.PUT, "/models/**/authz").permitAll()
			.mvcMatchers(HttpMethod.GET, "/models/**").permitAll()

			.antMatchers("/dashboard", "/dashboard/").hasRole(userRole) //logged in users may access their own dashboard
			.antMatchers("/dashboard/**").hasRole(adminRole) //only ADMIN may view dashboard for specific users
			.antMatchers("/usermodels/**").hasRole(adminRole) //only ADMIN may list models for specific users
			//jh17 : this has been changed to admin only so that it is not accessible to users
			.antMatchers("/domains/*/export", "/domains/ontologies").hasRole(adminRole)
			//jh17 : this is not perfect, a logged in user is able to see all domain models that exist
			.antMatchers("/domains/").hasRole(userRole)
			.antMatchers("/domains/**", "/domain-manager").hasRole(adminRole)
			.antMatchers("/administration/**", "/admin").hasRole(adminRole)

			// any other requests to ALL api (explisions were explicitly applied above) need to be fully authenticated
			.anyRequest().authenticated()
			.and()

			// configures default logout url at /logout
			.logout()
			.logoutUrl("/logout")
			.logoutSuccessUrl("/");
	}
}
