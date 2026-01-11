package com.example.ai.service;




import com.example.ai.pojo.SearchResult;

import java.util.List;

public interface SearXngService {
    public List<SearchResult> search(String query);

    public List<SearchResult> dealResult(List<SearchResult> results);

}
