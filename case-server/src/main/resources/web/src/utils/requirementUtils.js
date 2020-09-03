import _ from 'lodash';

export function getRequirmentAllInfos(projectLs, requirementLs, requirementId) {
  let initProject = { id: 0, name: '零散需求' };
  if (requirementLs.length === 0) {
    return;
  }
  let options = { projectLs: [], requirementLs: [] };
  let requirement = _.find(requirementLs, temp => {
    return temp.id === requirementId;
  });
  let project = null;
  if (requirement) {
    project = _.find(projectLs, temp => {
      return temp.id === requirement.iterationId;
    });
  }
  project = project || initProject;
  options.projectLs.push(project);
  options.requirementLs = requirementLs.filter(item => item.iterationId === project.id);
  return {
    project: project,
    requirement: requirement,
    options: options
  };
}
