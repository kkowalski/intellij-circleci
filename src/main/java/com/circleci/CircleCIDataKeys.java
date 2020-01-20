package com.circleci;

import com.circleci.api.model.Build;
import com.circleci.api.RequestExecutor;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.ui.CollectionComboBoxModel;

/**
 * @author Chris Kowalski
 */
public class CircleCIDataKeys {

    public static DataKey<BuildListLoader> listLoaderKey = DataKey.create("circleci.buildlist.loader");
    public static DataKey<Build> listSelectedBuildKey = DataKey.create("circleci.buildlist.selected");
    public static DataKey<CollectionComboBoxModel<String>> projectListModelKey = DataKey.create("circleci.projectlist.model");
    public static DataKey<RequestExecutor> requestExecutorKey = DataKey.create("circleci.request.executor");

}
