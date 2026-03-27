package be.axxes.timesheets.controller;

import be.axxes.timesheets.dto.ProjectDto;
import be.axxes.timesheets.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("currentPage", "projects");
        model.addAttribute("projects", projectService.getAllProjects());
        return "projects/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("currentPage", "projects");
        model.addAttribute("project", new ProjectDto());
        model.addAttribute("isEdit", false);
        return "projects/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("currentPage", "projects");
        model.addAttribute("project", projectService.toDto(projectService.getById(id)));
        model.addAttribute("isEdit", true);
        return "projects/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("project") ProjectDto dto,
                         BindingResult result,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("currentPage", "projects");
            model.addAttribute("isEdit", false);
            return "projects/form";
        }
        projectService.create(dto);
        redirectAttributes.addFlashAttribute("success", "Project aangemaakt");
        return "redirect:/projects";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("project") ProjectDto dto,
                         BindingResult result,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("currentPage", "projects");
            model.addAttribute("isEdit", true);
            return "projects/form";
        }
        projectService.update(id, dto);
        redirectAttributes.addFlashAttribute("success", "Project bijgewerkt");
        return "redirect:/projects";
    }

    @PostMapping("/{id}/archive")
    public String archive(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        projectService.archive(id);
        redirectAttributes.addFlashAttribute("success", "Project gearchiveerd");
        return "redirect:/projects";
    }

    @PostMapping("/{id}/activate")
    public String activate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        projectService.activate(id);
        redirectAttributes.addFlashAttribute("success", "Project geactiveerd");
        return "redirect:/projects";
    }
}
