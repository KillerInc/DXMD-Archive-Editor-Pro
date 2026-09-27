/** Dedicated normal-edit tab for confirmed DXMD XP/reward mappings. */
@SuppressWarnings("serial")
public class XPRewardPanel extends BaseEditPanel {
    public XPRewardPanel() {
        super("XP Rewards",
                "XP and reward controls. Ambiguous records stay in Research Inspector.",
                BaseOptionCatalog.xpRewards());
    }
}
