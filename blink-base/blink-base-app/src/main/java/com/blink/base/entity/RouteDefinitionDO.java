package com.blink.base.entity;


import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;


import org.springframework.validation.annotation.Validated;

import static org.springframework.util.StringUtils.tokenizeToStringArray;

/**
 * 类复制来自 gateway中的同名类 为了转换json
 * @Author binblink
 */
@Validated
public class RouteDefinitionDO {

    private String id;

    @NotEmpty
    @Valid
    private List<PredicateDefinitionDO> predicates = new ArrayList<>();

    @Valid
    private List<FilterDefinitionDO> filters = new ArrayList<>();

    @NotNull
    private URI uri;

    private Map<String, Object> metadata = new HashMap<>();

    private int order = 0;

    public RouteDefinitionDO() {
    }

    public RouteDefinitionDO(String text) {
        int eqIdx = text.indexOf('=');
        if (eqIdx <= 0) {
            throw new ValidationException(
                    "Unable to parse RouteDefinition text '" + text + "'" + ", must be of the form name=value");
        }

        setId(text.substring(0, eqIdx));

        String[] args = tokenizeToStringArray(text.substring(eqIdx + 1), ",");

        setUri(URI.create(args[0]));

        for (int i = 1; i < args.length; i++) {
            this.predicates.add(new PredicateDefinitionDO(args[i]));
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<PredicateDefinitionDO> getPredicates() {
        return predicates;
    }

    public void setPredicates(List<PredicateDefinitionDO> predicates) {
        this.predicates = predicates;
    }

    public List<FilterDefinitionDO> getFilters() {
        return filters;
    }

    public void setFilters(List<FilterDefinitionDO> filters) {
        this.filters = filters;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RouteDefinitionDO that = (RouteDefinitionDO) o;
        return this.order == that.order && Objects.equals(this.id, that.id)
                && Objects.equals(this.predicates, that.predicates) && Objects.equals(this.filters, that.filters)
                && Objects.equals(this.uri, that.uri) && Objects.equals(this.metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id, this.predicates, this.filters, this.uri, this.metadata, this.order);
    }

    @Override
    public String toString() {
        return "RouteDefinition{" + "id='" + id + '\'' + ", predicates=" + predicates + ", filters=" + filters
                + ", uri=" + uri + ", order=" + order + ", metadata=" + metadata + '}';
    }

}
