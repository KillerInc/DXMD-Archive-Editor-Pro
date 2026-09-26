/** Dedicated normal-edit tab for all confirmed DXMD XP/reward mappings. */
@SuppressWarnings("serial")
public class XPRewardPanel extends BaseEditPanel {
    public XPRewardPanel() {
        super("XP Rewards",
                "Confirmed DXMD XP/reward controls. Suspected or ambiguous reward records remain in Base Fields for research instead of being promoted here.",
                BaseOptionCatalog.xpRewards());
    }
}
