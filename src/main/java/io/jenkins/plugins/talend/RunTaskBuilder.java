package io.jenkins.plugins.talend;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.servlet.ServletException;

import org.apache.cxf.jaxrs.ext.search.client.SearchConditionBuilder;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import com.talend.tmc.dom.Executable;
import com.talend.tmc.dom.ExecutionRequest;
import com.talend.tmc.dom.ExecutionResponse;
import com.talend.tmc.dom.Task;
import com.talend.tmc.dom.Workspace;
import com.talend.tmc.services.TalendBearerAuth;
import com.talend.tmc.services.TalendCloudRegion;
import com.talend.tmc.services.TalendCredentials;
import com.talend.tmc.services.TalendRestException;
import com.talend.tmc.services.executables.ExecutableTask;
import com.talend.tmc.services.executables.ExecutableTaskService;
import com.talend.tmc.services.executions.ExecutionService;
import com.talend.tmc.services.workspaces.WorkspaceService;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.GlobalConfiguration;
import jenkins.tasks.SimpleBuildStep;

@Extension
public class RunTaskBuilder extends Builder implements SimpleBuildStep {
	@edu.umd.cs.findbugs.annotations.SuppressFBWarnings
	private static final Logger LOGGER = Logger.getLogger(GlobalConfiguration.class.getName());

	@edu.umd.cs.findbugs.annotations.SuppressFBWarnings
	private String tEnvironment;
	@edu.umd.cs.findbugs.annotations.SuppressFBWarnings
	private String tWorkspace;
	@edu.umd.cs.findbugs.annotations.SuppressFBWarnings
	private String tTask;

	@DataBoundConstructor
	public RunTaskBuilder() {
		this.tTask = "";
	}

	public String getEnvironment() {
		return tEnvironment;
	}

	@DataBoundSetter
	public void setEnvironment(String value) {
		this.tEnvironment = value;
	}

	public String getWorkspace() {
		return tWorkspace;
	}

	@DataBoundSetter
	public void setWorkspace(String value) {
		this.tWorkspace = value;
	}

	public String getTask() {
		return tTask;
	}

	@DataBoundSetter
	public void setTask(String value) {
		this.tTask = value;
	}

	@Override
	public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
			throws InterruptedException, IOException {
		listener.getLogger().println("*** RUNTASK ***");
		listener.getLogger().println("jobname=" + tTask);
		listener.getLogger().println("environment=" + tEnvironment);
		listener.getLogger().println("workspace=" + tWorkspace);
		listener.getLogger().println("*** RUNTASK ***");
		String id = "";
		Task task = null;

		TalendCredentials credentials = TalendLookupHelper.getTalendCredentials();
		TalendCloudRegion region = TalendLookupHelper.getTalendRegion();

		listener.getLogger().println("jobname=" + tTask);
		listener.getLogger().println("environment=" + tEnvironment);
		listener.getLogger().println("workspace=" + tWorkspace);

		try {
			ExecutionService executionService = ExecutionService.instance(credentials, region);

			ExecutableTask executableTask = ExecutableTask.instance(credentials, region);
			if (!tTask.isEmpty()) {
			   
				WorkspaceService workspaceService = WorkspaceService.instance(credentials, region);
				SearchConditionBuilder fiql = SearchConditionBuilder.instance("fiql");
				String query = fiql.is("name").equalTo(tWorkspace).and().is("environment.name").equalTo(tEnvironment)
						.query();
				Workspace[] workspaces = workspaceService.get(query);
				if (workspaces.length > 1)
					listener.getLogger()
							.println("More than 1 workspace returned with that name! We'll take the first one.");
				String workspaceId = workspaces[0].getId();
				listener.getLogger().println("workspaceId=" + workspaceId);
				Task[] tasks = null;
				tasks = executableTask.getByName(tTask, workspaceId);
				task = tasks[0];
				listener.getLogger().println("task=" + task.getName());
				id = task.getExecutable();

			} else {
				listener.getLogger().println("No Task provided!");
			}

			listener.getLogger().println("Found Task: " + id);

			ExecutionRequest executionRequest = new ExecutionRequest();
			executionRequest.setExecutable(id);

			/*
			 * TODO: Read all the logs until finished
			 * 
			 * Hashtable<String, String> parameters = null; if (Cli.hasCliValue("cv")) {
			 * String[] pairs = Cli.getCliValue("cv").split(";"); parameters = new
			 * Hashtable<>(); for (String pair : pairs) { String[] nv = pair.split("=");
			 * parameters.put(nv[0], nv[1]); }
			 * 
			 * executionRequest.setParameters(parameters); }
			 * 
			 */
			ExecutionResponse executionResponse = executionService.post(executionRequest);
			listener.getLogger().println("Talend Task Started: " + executionResponse.getExecutionId());

			Thread.sleep(10); // to include the InterruptedException
		} catch (RuntimeException e) {
			throw e;
		} catch (TalendRestException | IOException | InterruptedException ex) {
			listener.getLogger().println(ex.getMessage());
		} catch (Exception e) {
			listener.getLogger().println(e.getMessage());
		}

	}

	@Extension
	@Symbol("runTask")
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

		public ListBoxModel doFillEnvironmentItems(@CheckForNull @AncestorInPath Item context) {
			return TalendLookupHelper.getEnvironmentList();
		}

		public ListBoxModel doFillWorkspaceItems(@QueryParameter String environment) {
			return TalendLookupHelper.getWorkspaceList(environment);
		}

		public ListBoxModel doFillTaskItems(@QueryParameter String environment, @QueryParameter String workspace) {
			return TalendLookupHelper.getTaskList(environment, workspace);
		}

		public FormValidation doCheckName(@QueryParameter String environment, @QueryParameter String workspace,
				@QueryParameter String task) throws IOException, ServletException {
			if (task.length() == 0)
				return FormValidation.warning("No Job");
			if (environment.length() < 4)
				return FormValidation.warning("No Env");
			if (workspace.length() < 4) {
				return FormValidation.warning("No Workspace");
			}
			return FormValidation.ok();
		}

		@Override
		public boolean isApplicable(@SuppressWarnings("rawtypes") Class<? extends AbstractProject> aClass) {
			return true;
		}

		@Override
		public String getDisplayName() {
			return Messages.RunTaskBuilder_DescriptorImpl_DisplayName();
		}

	}

}
