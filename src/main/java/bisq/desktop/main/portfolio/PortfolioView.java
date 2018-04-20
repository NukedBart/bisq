/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.desktop.main.portfolio;

import bisq.desktop.Navigation;
import bisq.desktop.common.model.Activatable;
import bisq.desktop.common.view.ActivatableViewAndModel;
import bisq.desktop.common.view.CachingViewLoader;
import bisq.desktop.common.view.FxmlView;
import bisq.desktop.common.view.View;
import bisq.desktop.common.view.ViewLoader;
import bisq.desktop.main.MainView;
import bisq.desktop.main.portfolio.closedtrades.ClosedTradesView;
import bisq.desktop.main.portfolio.editoffer.EditOpenOfferView;
import bisq.desktop.main.portfolio.failedtrades.FailedTradesView;
import bisq.desktop.main.portfolio.openoffer.OpenOffersView;
import bisq.desktop.main.portfolio.pendingtrades.PendingTradesView;

import bisq.core.locale.Res;
import bisq.core.offer.OpenOffer;
import bisq.core.trade.Trade;
import bisq.core.trade.failed.FailedTradesManager;

import javax.inject.Inject;

import javafx.fxml.FXML;

import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import javafx.beans.value.ChangeListener;

import javafx.collections.ListChangeListener;

import java.util.List;

@FxmlView
public class PortfolioView extends ActivatableViewAndModel<TabPane, Activatable> {

    @FXML
    Tab openOffersTab, pendingTradesTab, closedTradesTab;
    private Tab editOpenOfferTab;
    private final Tab failedTradesTab = new Tab(Res.get("portfolio.tab.failed"));
    private Tab currentTab;
    private Navigation.Listener navigationListener;
    private ChangeListener<Tab> tabChangeListener;
    private ListChangeListener<Tab> tabListChangeListener;

    private final ViewLoader viewLoader;
    private final Navigation navigation;
    private final FailedTradesManager failedTradesManager;
    private EditOpenOfferView editOpenOfferView;
    private boolean editOpenOfferViewOpen;
    private OpenOffer openOffer;
    private OpenOffersView openOffersView;

    @Inject
    public PortfolioView(CachingViewLoader viewLoader, Navigation navigation, FailedTradesManager failedTradesManager) {
        this.viewLoader = viewLoader;
        this.navigation = navigation;
        this.failedTradesManager = failedTradesManager;
    }

    @Override
    public void initialize() {
        openOffersTab.setText(Res.get("portfolio.tab.openOffers"));
        pendingTradesTab.setText(Res.get("portfolio.tab.pendingTrades"));
        closedTradesTab.setText(Res.get("portfolio.tab.history"));

        navigationListener = viewPath -> {
            if (viewPath.size() == 3 && viewPath.indexOf(PortfolioView.class) == 1)
                loadView(viewPath.tip());
        };

        tabChangeListener = (ov, oldValue, newValue) -> {
            if (newValue == openOffersTab)
                //noinspection unchecked
                navigation.navigateTo(MainView.class, PortfolioView.class, OpenOffersView.class);
            else if (newValue == pendingTradesTab)
                //noinspection unchecked
                navigation.navigateTo(MainView.class, PortfolioView.class, PendingTradesView.class);
            else if (newValue == closedTradesTab)
                //noinspection unchecked
                navigation.navigateTo(MainView.class, PortfolioView.class, ClosedTradesView.class);
            else if (newValue == failedTradesTab)
                //noinspection unchecked
                navigation.navigateTo(MainView.class, PortfolioView.class, FailedTradesView.class);
            else if (newValue == editOpenOfferTab) {
                //noinspection unchecked
                navigation.navigateTo(MainView.class, PortfolioView.class, EditOpenOfferView.class);
            }

            if (oldValue != null && oldValue == editOpenOfferTab)
                editOpenOfferView.onTabSelected(false);

        };

        tabListChangeListener = change -> {
            change.next();
            List<? extends Tab> removedTabs = change.getRemoved();
            if (removedTabs.size() == 1 && removedTabs.get(0).equals(editOpenOfferTab))
                onEditOpenOfferRemoved();
        };
    }

    private void onEditOpenOfferRemoved() {
        editOpenOfferViewOpen = false;
        if (editOpenOfferView != null) {
            editOpenOfferView.onClose();
            editOpenOfferView = null;
        }

        //noinspection unchecked
        navigation.navigateTo(MainView.class, this.getClass(), OpenOffersView.class);
    }

    @Override
    protected void activate() {
        failedTradesManager.getFailedTrades().addListener((ListChangeListener<Trade>) c -> {
            if (failedTradesManager.getFailedTrades().size() > 0 && root.getTabs().size() == 3)
                root.getTabs().add(failedTradesTab);
        });
        if (failedTradesManager.getFailedTrades().size() > 0 && root.getTabs().size() == 3)
            root.getTabs().add(failedTradesTab);

        root.getSelectionModel().selectedItemProperty().addListener(tabChangeListener);
        root.getTabs().addListener(tabListChangeListener);
        navigation.addListener(navigationListener);

        if (root.getSelectionModel().getSelectedItem() == openOffersTab)
            //noinspection unchecked
            navigation.navigateTo(MainView.class, PortfolioView.class, OpenOffersView.class);
        else if (root.getSelectionModel().getSelectedItem() == pendingTradesTab)
            //noinspection unchecked
            navigation.navigateTo(MainView.class, PortfolioView.class, PendingTradesView.class);
        else if (root.getSelectionModel().getSelectedItem() == closedTradesTab)
            //noinspection unchecked
            navigation.navigateTo(MainView.class, PortfolioView.class, ClosedTradesView.class);
        else if (root.getSelectionModel().getSelectedItem() == failedTradesTab)
            //noinspection unchecked
            navigation.navigateTo(MainView.class, PortfolioView.class, FailedTradesView.class);
        else if (root.getSelectionModel().getSelectedItem() == editOpenOfferTab) {
            //noinspection unchecked
            navigation.navigateTo(MainView.class, PortfolioView.class, EditOpenOfferView.class);
            if (editOpenOfferView != null) editOpenOfferView.onTabSelected(true);
        }
    }

    @Override
    protected void deactivate() {
        root.getSelectionModel().selectedItemProperty().removeListener(tabChangeListener);
        root.getTabs().removeListener(tabListChangeListener);
        navigation.removeListener(navigationListener);
        currentTab = null;
    }

    private void loadView(Class<? extends View> viewClass) {
        // we want to get activate/deactivate called, so we remove the old view on tab change
        // TODO Don't understand the check for currentTab != editOpenOfferTab
        if (currentTab != null && currentTab != editOpenOfferTab)
            currentTab.setContent(null);

        View view = viewLoader.load(viewClass);

        if (view instanceof OpenOffersView) {
            selectOpenOffersView((OpenOffersView) view);
        } else if (view instanceof PendingTradesView) {
            currentTab = pendingTradesTab;
        } else if (view instanceof ClosedTradesView) {
            currentTab = closedTradesTab;
        } else if (view instanceof FailedTradesView) {
            currentTab = failedTradesTab;
        } else if (view instanceof EditOpenOfferView) {
            if (openOffer != null) {
                if (editOpenOfferView == null) {
                    editOpenOfferView = (EditOpenOfferView) view;
                    editOpenOfferView.initWithData(openOffer);
                    editOpenOfferTab = new Tab(Res.get("portfolio.tab.editOpenOffer"));
                    editOpenOfferView.setCloseHandler(() -> {
                        root.getTabs().remove(editOpenOfferTab);
                    });
                    root.getTabs().add(editOpenOfferTab);
                }
                if (currentTab != editOpenOfferTab)
                    editOpenOfferView.onTabSelected(true);

                currentTab = editOpenOfferTab;
            } else {
                view = viewLoader.load(OpenOffersView.class);
                selectOpenOffersView((OpenOffersView) view);
            }
        }

        currentTab.setContent(view.getRoot());
        root.getSelectionModel().select(currentTab);
    }

    private void selectOpenOffersView(OpenOffersView view) {
        openOffersView = view;
        currentTab = openOffersTab;

        OpenOfferActionHandler openOfferActionHandler = openOffer -> {
            if (!editOpenOfferViewOpen) {
                editOpenOfferViewOpen = true;
                PortfolioView.this.openOffer = openOffer;
                navigation.navigateTo(MainView.class, PortfolioView.this.getClass(), EditOpenOfferView.class);
            } else {
                log.error("You have already a \"Edit Offer\" tab open.");
            }
        };
        openOffersView.setOpenOfferActionHandler(openOfferActionHandler);
    }

    public interface OpenOfferActionHandler {
        void onEditOpenOffer(OpenOffer openOffer);
    }
}

