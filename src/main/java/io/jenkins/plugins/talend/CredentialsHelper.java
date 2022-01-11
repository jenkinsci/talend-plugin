package io.jenkins.plugins.talend;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import hudson.model.Item;
import hudson.model.Queue;
import hudson.model.queue.Tasks;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;


/**
 * Helper class for vary credentials operations.
 *
 * @author Zhenlei Huang
 * From the jira-plugin
 */
public class CredentialsHelper {
	private static final Logger LOGGER = Logger.getLogger(CredentialsHelper.class.getName());
	static private String url = "http://talend.com";
	@edu.umd.cs.findbugs.annotations.CheckForNull
	protected static StringCredentials lookupSystemCredentials(@edu.umd.cs.findbugs.annotations.CheckForNull String credentialsId) {
		if (credentialsId == null) {
			return null;
		}
		return CredentialsMatchers.firstOrNull(
				CredentialsProvider.lookupCredentials(
						StringCredentials.class,
						Jenkins.get(),
						ACL.SYSTEM,
						URIRequirementBuilder.fromUri(url).build()
				),
				CredentialsMatchers.withId(credentialsId)
		);
	}

	protected static ListBoxModel doFillCredentialsIdItems( Item item, String credentialsId) {
		StandardListBoxModel result = new StandardListBoxModel();
		if (item == null) {
			if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
				return result.includeCurrentValue(credentialsId);
			}
		} else {
			if (!item.hasPermission(Item.EXTENDED_READ)
				&& !item.hasPermission(CredentialsProvider.USE_ITEM)) {
				return result.includeCurrentValue(credentialsId);
			}
		}
		return result //
			.includeEmptyValue() //
            .includeMatchingAs(
                item instanceof Queue.Task ? Tasks.getAuthenticationOf( (Queue.Task) item) : ACL.SYSTEM,
                item,
                StandardCredentials.class,
                URIRequirementBuilder.fromUri( url).build(),
                CredentialsMatchers.anyOf(
					CredentialsMatchers.instanceOf(StringCredentials.class)
					))
			.includeCurrentValue(credentialsId);
	}

	protected static FormValidation doCheckFillCredentialsId(
		Item item, String credentialsId) {
		if (item == null) {
			if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
				return FormValidation.ok();
			}
		} else {
			if (!item.hasPermission(Item.EXTENDED_READ)
				&& !item.hasPermission(CredentialsProvider.USE_ITEM)) {
				return FormValidation.ok();
			}
		}
		if (StringUtils.isEmpty(credentialsId)) {
			return FormValidation.ok();
		}
		if (!(findCredentials(item, credentialsId).isPresent())) {
			return FormValidation.error("Cannot find currently selected credentials");
		}
		return FormValidation.ok();
	}

	protected static Optional<StringCredentials> findCredentials(
		Item item, String credentialsId) {
		return Optional.ofNullable(
			CredentialsMatchers.firstOrNull(
				CredentialsProvider.lookupCredentials(
					StringCredentials.class,
					item,
					item instanceof Queue.Task
						? Tasks.getAuthenticationOf( (Queue.Task) item)
						: ACL.SYSTEM,
					URIRequirementBuilder.fromUri(url).build()),
			CredentialsMatchers.withId(credentialsId)
		));
	}

}
