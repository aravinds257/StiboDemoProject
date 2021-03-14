package com.stibo.demo.report.service;

import com.stibo.demo.report.logging.LogTime;
import com.stibo.demo.report.model.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

@Service
public class ReportService {

    @LogTime
    public Stream<Stream<String>> report(Datastandard datastandard, String categoryId) {
        // TODO: implement
        Category category = findCategoryById(datastandard, categoryId);
        return getReportForCategories(datastandard, category);
    }

    @NotNull
    private Category findCategoryById(@NotNull Datastandard datastandard, @NotNull String categoryId) {
        List<Category> categories = datastandard.getCategories().stream()
                .filter(category -> category.getId().equals(categoryId))
                .collect(Collectors.toList());

        return categories.get(0);
    }

    @NotNull
    private Stream<Stream<String>> getReportForCategories(@NotNull Datastandard datastandard,
                                                                     @NotNull Category category) {

        Map<String, AttributeLink> attrIdAttrLink = getAttributeLinkMap(category.getAttributeLinks());

        List<Attribute> attributes = getAttributesForAttributeLinks(datastandard, attrIdAttrLink);

        List<List<String>> report = attributes.stream()
                .map(attribute -> {

                    List<String> row = new ArrayList<>(5);
                    //Part1
                    row.add(" | ");
                    row.add(category.getName());
                    row.add(" | ");
                    row.add(getAttributeName(attribute, attrIdAttrLink));
                    row.add(" | ");
                    row.add(getAttributeDescription(attribute));
                    row.add(" | ");
                    row.add(getAttributeTypeId(datastandard, attribute));
                    row.add(" | ");
                    //Part2
                    row.add(getAttributeGroupsNames(datastandard, attribute));
                    row.add(" | ");

                    return row;
                })
                .collect(Collectors.toList());

        return prepareResultsAndCheckParent(datastandard, category, report);

    }

    @NotNull
    private List<Attribute> getAttributesForAttributeLinks(@NotNull Datastandard datastandard, Map<String, AttributeLink> attrIdAttrLink) {
        return datastandard.getAttributes().stream()
                .filter(attribute -> attrIdAttrLink.containsKey(attribute.getId()))
                .collect(Collectors.toList());
    }

    @NotNull
    private Map<String, AttributeLink> getAttributeLinkMap(List<AttributeLink> attributeLinkList) {
        return attributeLinkList.stream()
                .collect(Collectors.toMap(AttributeLink::getId, attributeLink -> attributeLink));
    }

    @NotNull
    private Stream<Stream<String>> prepareResultsAndCheckParent(@NotNull Datastandard datastandard,
                                                                @NotNull Category category,
                                                                @NotNull List<List<String>> report) {

        Stream<Stream<String>> results = report.stream().map(Collection::stream);

        if (category.getParentId() != null) {
            Category parent = findCategoryById(datastandard, category.getParentId());

            // recursive call, going up in hierarchy to parent category
            return Stream.concat(results, getReportForCategories(datastandard, parent));

        } else
            return results;
    }

    @NotNull
    private String getAttributeName(@NotNull Attribute attribute, @NotNull Map<String, AttributeLink> attrLinksById) {

        return attrLinksById.get(attribute.getId()).getOptional()
                ? attribute.getName()
                : attribute.getName() + "*";
    }

    @NotNull
    private String getAttributeDescription(@NotNull Attribute attribute) {

        return attribute.getDescription() != null
                ? attribute.getDescription()
                : "";
    }

    @NotNull
    private String getAttributeTypeId(@NotNull Datastandard datastandard, @NotNull Attribute attribute) {

        return attribute.getType().getMultiValue()
                ? attribute.getType().getId() + "[]"
                : attribute.getType().getId();
    }

    @NotNull
    public List<Attribute> getAllLinkedAttributes(@NotNull Datastandard datastandard,
                                                  @NotNull Attribute attribute) {

        Set<String> links = attribute.getAttributeLinks().stream()
                .map(AttributeLink::getId)
                .collect(Collectors.toSet());

        return datastandard.getAttributes().stream()
                .filter(attr -> links.contains(attr.getId()))
                .collect(Collectors.toList());
    }

    @NotNull
    private String getAttributeGroupsNames(@NotNull Datastandard datastandard, @NotNull Attribute attribute) {

        return getAllAttributesGroupsNames(datastandard, attribute);

    }

    @NotNull
    public String getAllAttributesGroupsNames(@NotNull Datastandard datastandard,
                                              @NotNull Attribute attribute) {

        // to avoid O(n) complexity on List::contains, will be slower for small sets
        Set<String> groupIds = new HashSet<>(attribute.getGroupIds());

        return datastandard.getAttributeGroups().stream()
                .filter(ag -> groupIds.contains(ag.getId()))
                .map(AttributeGroup::getName)
                .collect(joining("\n"));
    }

    //Part 3
    @NotNull
    private String getAttributeTypes(@NotNull Datastandard datastandard, @NotNull Attribute attribute) {

        List<AttributeLink> attributeLinks = attribute.getAttributeLinks();

        if (attributeLinks == null || attributeLinks.size() == 0)
            return attribute.getType().getId();
        else {
            Map<String, AttributeLink> attrIdAttrLink = getAttributeLinkMap(attributeLinks);

            List<Attribute> attributes = getAllLinkedAttributes(datastandard, attribute);
            List<String>  attributeLinksForAttributeId = new ArrayList<>();

            return attribute.getType().getId() + "{\n" + attributeLinksForAttributeId + "\n}";
        }

    }
}
