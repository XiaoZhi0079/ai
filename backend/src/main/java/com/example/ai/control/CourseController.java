package com.example.ai.control;

import com.example.ai.entity.Course;
import com.example.ai.pojo.LeeResult;
import com.example.ai.service.CourseService;
import com.example.ai.security.Role;
import com.example.ai.security.RoleRequired;
import com.example.ai.utils.OperatorResolver;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @RoleRequired({Role.ADMIN, Role.TEACHER})
    @PostMapping
    public LeeResult<Course> create(HttpServletRequest request,
                                    @RequestBody Course course) {
        return LeeResult.ok(courseService.create(course, OperatorResolver.resolve(request)));
    }

    @RoleRequired({Role.ADMIN, Role.TEACHER, Role.STUDENT})
    @GetMapping
    public LeeResult<List<Course>> list() {
        return LeeResult.ok(courseService.list());
    }

    @RoleRequired({Role.ADMIN, Role.TEACHER, Role.STUDENT})
    @GetMapping("/{id}")
    public LeeResult<Course> get(@PathVariable Integer id) {
        return courseService.get(id)
                .map(LeeResult::ok)
                .orElseGet(() -> LeeResult.fail("Course not found"));
    }

    @RoleRequired({Role.ADMIN, Role.TEACHER})
    @PutMapping("/{id}")
    public LeeResult<Course> update(HttpServletRequest request,
                                    @PathVariable Integer id,
                                    @RequestBody Course course) {
        return courseService.update(id, course, OperatorResolver.resolve(request))
                .map(LeeResult::ok)
                .orElseGet(() -> LeeResult.fail("Course not found"));
    }

    @RoleRequired({Role.ADMIN, Role.TEACHER})
    @DeleteMapping("/{id}")
    public LeeResult<Void> delete(HttpServletRequest request,
                                  @PathVariable Integer id) {
        boolean deleted = courseService.delete(id, OperatorResolver.resolve(request));
        return deleted ? LeeResult.ok() : LeeResult.fail("Course not found");
    }
}
