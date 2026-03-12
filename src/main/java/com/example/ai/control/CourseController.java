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
                                    @RequestBody Course course,
                                    @RequestHeader(value = "X-User", required = false) String operator,
                                    @RequestHeader(value = "X-Role", required = false) String role) {
        try {
            return LeeResult.ok(courseService.create(course, OperatorResolver.resolve(request, operator, role)));
        } catch (IllegalArgumentException ex) {
            return LeeResult.fail(ex.getMessage());
        }
    }

    @RoleRequired({Role.ADMIN, Role.TEACHER})
    @GetMapping
    public LeeResult<List<Course>> list() {
        return LeeResult.ok(courseService.list());
    }

    @RoleRequired({Role.ADMIN, Role.TEACHER})
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
                                    @RequestBody Course course,
                                    @RequestHeader(value = "X-User", required = false) String operator,
                                    @RequestHeader(value = "X-Role", required = false) String role) {
        try {
            return courseService.update(id, course, OperatorResolver.resolve(request, operator, role))
                    .map(LeeResult::ok)
                    .orElseGet(() -> LeeResult.fail("Course not found"));
        } catch (IllegalArgumentException ex) {
            return LeeResult.fail(ex.getMessage());
        }
    }

    @RoleRequired({Role.ADMIN, Role.TEACHER})
    @DeleteMapping("/{id}")
    public LeeResult<Void> delete(HttpServletRequest request,
                                  @PathVariable Integer id,
                                  @RequestHeader(value = "X-User", required = false) String operator,
                                  @RequestHeader(value = "X-Role", required = false) String role) {
        boolean deleted = courseService.delete(id, OperatorResolver.resolve(request, operator, role));
        return deleted ? LeeResult.ok() : LeeResult.fail("Course not found");
    }
}
