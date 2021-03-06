package sonnicon.sonnicore.ui;

import io.anuke.arc.Core;
import io.anuke.arc.Events;
import io.anuke.arc.collection.Array;
import io.anuke.arc.collection.ObjectMap;
import io.anuke.arc.func.Cons;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.math.geom.Vector2;
import io.anuke.arc.scene.Element;
import io.anuke.arc.scene.Group;
import io.anuke.arc.scene.event.Touchable;
import io.anuke.arc.scene.style.TextureRegionDrawable;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.scene.ui.layout.Scl;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.arc.util.Time;
import io.anuke.arc.util.Tmp;
import io.anuke.mindustry.entities.traits.BuilderTrait.BuildRequest;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.game.EventType.BlockInfoEvent;
import io.anuke.mindustry.game.EventType.ResetEvent;
import io.anuke.mindustry.game.EventType.UnlockEvent;
import io.anuke.mindustry.game.EventType.WorldLoadEvent;
import io.anuke.mindustry.gen.Icon;
import io.anuke.mindustry.gen.Tex;
import io.anuke.mindustry.graphics.Pal;
import io.anuke.mindustry.input.Binding;
import io.anuke.mindustry.input.InputHandler;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.ui.Cicon;
import io.anuke.mindustry.ui.Styles;
import io.anuke.mindustry.ui.fragments.HudFragment;
import io.anuke.mindustry.ui.fragments.PlacementFragment;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import sonnicon.sonnicore.types.ModCategory;
import sonnicon.sonnicore.world.BlockUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static io.anuke.mindustry.Vars.*;

public class ModPlacementFragment extends PlacementFragment{
    final int rowWidth = 4;

    public ModCategory currentCategory = ModCategory.get("distribution");
    Array<Block> returnArray = new Array<>();
    Array<ModCategory> returnCatArray = new Array<>();
    boolean[] categoryEmpty;
    ObjectMap<ModCategory, Block> selectedBlocks = new ObjectMap<ModCategory, Block>();
    Block hovered, lastDisplay;
    Tile lastHover;
    Tile hoverTile;
    Table blockTable, toggler, topTable;
    boolean lastGround;
    boolean blockSelectEnd;
    int blockSelectSeq;
    long blockSelectSeqMillis;
    Binding[] blockSelect = {
        Binding.block_select_01,
        Binding.block_select_02,
        Binding.block_select_03,
        Binding.block_select_04,
        Binding.block_select_05,
        Binding.block_select_06,
        Binding.block_select_07,
        Binding.block_select_08,
        Binding.block_select_09,
        Binding.block_select_10,
        Binding.block_select_left,
        Binding.block_select_right,
        Binding.block_select_up,
        Binding.block_select_down
    };

    public String search = "";
    boolean searchChanged = false;

    public ModPlacementFragment(){
        super();
        Events.on(WorldLoadEvent.class, event -> {
            Core.app.post(() -> {
                control.input.block = null;
                rebuild();
            });
        });

        Events.on(UnlockEvent.class, event -> {
            if(event.content instanceof Block){
                rebuild();
            }
        });

        Events.on(ResetEvent.class, event -> {
            selectedBlocks.clear();
        });
    }

    void rebuild(){
        currentCategory = ModCategory.get("turret");
        Group group = toggler.getParent();
        int index = toggler.getZIndex();
        toggler.remove();
        build(group);
        toggler.setZIndex(index);
    }

    boolean gridUpdate(InputHandler input){
        if(Core.input.keyDown(Binding.pick)){ //mouse eyedropper select
            Tile tile = world.ltileWorld(Core.input.mouseWorld().x, Core.input.mouseWorld().y);
            Block tryRecipe = tile == null ? null : tile.block();

            for(BuildRequest req : player.buildQueue()){
                if(!req.breaking && req.block.bounds(req.x, req.y, Tmp.r1).contains(Core.input.mouseWorld())){
                    tryRecipe = req.block;
                    break;
                }
            }

            if(tryRecipe != null && tryRecipe.isVisible() && unlocked(tryRecipe)){
                input.block = tryRecipe;
                currentCategory = input.block.category == null ? BlockUtils.getByBlock(input.block) : ModCategory.get(input.block.category.name());
                return true;
            }
        }

        if(ui.chatfrag.chatOpen()) return false;
        for(int i = 0; i < blockSelect.length; i++){
            if(Core.input.keyTap(blockSelect[i])){
                if(i > 9) { //select block directionally
                    Array<Block> blocks = getByCategory(currentCategory);
                    Block currentBlock = getSelectedBlock(currentCategory);
                    for(int j = 0; j < blocks.size; j++){
                        if(blocks.get(j) == currentBlock){
                            switch(i){
                                case 10: //left
                                    j = (j - 1 + blocks.size) % blocks.size;
                                    break;
                                case 11: //right
                                    j = (j + 1) % blocks.size;
                                    break;
                                case 12: //up
                                    j = (j > 3 ? j - 4 : blocks.size - blocks.size % 4 + j);
                                    j -= (j < blocks.size ? 0 : 4);
                                    break;
                                case 13: //down
                                    j = (j < blocks.size - 4 ? j + 4 : j % 4);
                            }
                            input.block = blocks.get(j);
                            selectedBlocks.put(currentCategory, input.block);
                            break;
                        }
                    }
                }else if(blockSelectEnd || Time.timeSinceMillis(blockSelectSeqMillis) > Core.settings.getInt("blockselecttimeout")){ //1st number of combo, select category
                    //select only visible categories
                    if(!getByCategory(ModCategory.get(i)).isEmpty()){
                        currentCategory = ModCategory.get(i);
                        if(input.block != null){
                            input.block = getSelectedBlock(currentCategory);
                        }
                        blockSelectEnd = false;
                        blockSelectSeq = 0;
                        blockSelectSeqMillis = Time.millis();
                    }
                }else{ //select block
                    if(blockSelectSeq == 0){ //2nd number of combo
                        blockSelectSeq = i + 1;
                    }else{ //3rd number of combo
                        //entering "X,1,0" selects the same block as "X,0"
                        i += (blockSelectSeq - (i != 9 ? 0 : 1)) * 10;
                        blockSelectEnd = true;
                    }
                    Array<Block> blocks = getByCategory(currentCategory);
                    input.block = (i < blocks.size) ? blocks.get(i) : null;
                    selectedBlocks.put(currentCategory, input.block);
                    blockSelectSeqMillis = Time.millis();
                }
                return true;
            }
        }

        if(Core.input.keyTap(Binding.category_prev)){
            do{
                currentCategory = currentCategory.prev();
            }while(categoryEmpty[ModCategory.indexOf(currentCategory)]);
            input.block = getSelectedBlock(currentCategory);
            return true;
        }

        if(Core.input.keyTap(Binding.category_next)){
            do{
                currentCategory = currentCategory.next();
            }while(categoryEmpty[ModCategory.indexOf(currentCategory)]);
            input.block = getSelectedBlock(currentCategory);
            return true;
        }

        return false;
    }

    @Override
    public void build(Group parent){
        parent.fill(full -> {
            toggler = full;
            full.bottom().right().visible(() -> ui.hudfrag.shown());
            full.table(frame -> {
                //rebuilds the category table with the correct recipes
                Runnable rebuildCategory = () -> {
                    blockTable.clear();
                    blockTable.top().margin(5);

                    int index = 0;

                    ButtonGroup<ImageButton> group = new ButtonGroup<>();
                    group.setMinCheckCount(0);

                    for(Block block : getByCategory(currentCategory)){
                        if(index++ % rowWidth == 0){
                            blockTable.row();
                        }

                        ImageButton button = blockTable.addImageButton(Icon.lockedSmall, Styles.selecti, () -> {
                            if(unlocked(block)){
                                control.input.block = control.input.block == block ? null : block;
                                selectedBlocks.put(currentCategory, control.input.block);
                            }
                        }).size(46f).group(group).name("block-" + block.name).get();

                        button.getStyle().imageUp = new TextureRegionDrawable(block.icon(Cicon.medium));

                        button.update(() -> { //color unplacable things gray
                            TileEntity core = player.getClosestCore();
                            Color color = state.rules.infiniteResources || (core != null && (core.items.has(block.requirements, state.rules.buildCostMultiplier) || state.rules.infiniteResources)) ? Color.white : Color.gray;
                            button.forEach(elem -> elem.setColor(color));
                            button.setChecked(control.input.block == block);

                            if(state.rules.bannedBlocks.contains(block)){
                                button.forEach(elem -> elem.setColor(Color.darkGray));
                            }
                        });

                        button.hovered(() -> hovered = block);
                        button.exited(() -> {
                            if(hovered == block){
                                hovered = null;
                            }
                        });
                    }
                    //add missing elements to even out table size
                    if(index < 4){
                        for(int i = 0; i < 4-index; i++){
                            blockTable.add().size(46f);
                        }
                    }
                    blockTable.act(0f);
                };
                //top table with hover info
                frame.table(Tex.buttonEdge2, top -> {
                    topTable = top;
                    top.add(new Table()).growX().update(topTable -> {
                        //don't refresh unnecessarily
                        if((tileDisplayBlock() == null && lastDisplay == getSelected() && !lastGround)
                        || (tileDisplayBlock() != null && lastHover == hoverTile && lastDisplay == tileDisplayBlock() && lastGround))
                            return;

                        topTable.clear();
                        topTable.top().left().margin(5);

                        lastHover = hoverTile;
                        lastDisplay = getSelected();
                        lastGround = tileDisplayBlock() != null;

                        if(lastDisplay != null){ //show selected recipe
                            lastGround = false;

                            topTable.table(header -> {
                                String keyCombo = "";
                                if(!mobile && Core.settings.getBool("blockselectkeys")){
                                    Array<Block> blocks = getByCategory(currentCategory);
                                    for(int i = 0; i < blocks.size; i++){
                                        if(blocks.get(i) == lastDisplay){
                                            keyCombo = Core.bundle.format("placement.blockselectkeys", Core.keybinds.get(blockSelect[ModCategory.indexOf(currentCategory)]).key.toString())
                                                + (i < 10 ? "" : Core.keybinds.get(blockSelect[(i + 1) / 10 - 1]).key.toString() + ",")
                                                + Core.keybinds.get(blockSelect[i % 10]).key.toString() + "]";
                                            break;
                                        }
                                    }
                                }
                                final String keyComboFinal = keyCombo;
                                header.left();
                                header.add(new Image(lastDisplay.icon(Cicon.medium))).size(8 * 4);
                                header.labelWrap(() -> !unlocked(lastDisplay) ? Core.bundle.get("block.unknown") : lastDisplay.localizedName + keyComboFinal)
                                .left().width(190f).padLeft(5);
                                header.add().growX();
                                if(unlocked(lastDisplay)){
                                    header.addButton("?", Styles.clearPartialt, () -> {
                                        ui.content.show(lastDisplay);
                                        Events.fire(new BlockInfoEvent());
                                    }).size(8 * 5).padTop(-5).padRight(-5).right().grow().name("blockinfo");
                                }
                            }).growX().left();
                            topTable.row();
                            //add requirement table
                            topTable.table(req -> {
                                req.top().left();

                                for(ItemStack stack : lastDisplay.requirements){
                                    req.table(line -> {
                                        line.left();
                                        line.addImage(stack.item.icon(Cicon.small)).size(8 * 2);
                                        line.add(stack.item.localizedName).maxWidth(140f).fillX().color(Color.lightGray).padLeft(2).left().get().setEllipsis(true);
                                        line.labelWrap(() -> {
                                            TileEntity core = player.getClosestCore();
                                            if(core == null || state.rules.infiniteResources) return "*/*";

                                            int amount = core.items.get(stack.item);
                                            int stackamount = Math.round(stack.amount * state.rules.buildCostMultiplier);
                                            String color = (amount < stackamount / 2f ? "[red]" : amount < stackamount ? "[accent]" : "[white]");

                                            return color + ui.formatAmount(amount) + "[white]/" + stackamount;
                                        }).padLeft(5);
                                    }).left();
                                    req.row();
                                }
                            }).growX().left().margin(3);

                            if(state.rules.bannedBlocks.contains(lastDisplay)){
                                topTable.row();
                                topTable.table(b -> {
                                    b.addImage(Icon.cancelSmall).padRight(2).color(Color.scarlet);
                                    b.add("$banned");
                                    b.left();
                                }).padTop(2).left();
                            }

                        }else if(tileDisplayBlock() != null){ //show selected tile
                            lastDisplay = tileDisplayBlock();
                            topTable.table(t -> {
                                t.left();
                                t.add(new Image(lastDisplay.getDisplayIcon(hoverTile))).size(8 * 4);
                                t.labelWrap(lastDisplay.getDisplayName(hoverTile)).left().width(190f).padLeft(5);
                            }).growX().left();
                            if(hoverTile.getTeam() == player.getTeam()){
                                topTable.row();
                                topTable.table(t -> {
                                    t.left().defaults().left();
                                    lastDisplay.display(hoverTile, t);
                                }).left().growX();
                            }
                        }
                    });
                }).colspan(3).fillX().visible(() -> getSelected() != null || tileDisplayBlock() != null).touchable(Touchable.enabled);
                frame.row();
                frame.addImage().color(Pal.gray).colspan(3).height(4).growX();
                frame.row();
                frame.table(Tex.pane2, searchtab -> {
                    searchtab.left();
                    searchtab.addField(search, text -> {
                        search = text;
                        searchChanged = true;
                    }).pad(-2f).fillX().get().setFillParent(true);
                }).colspan(3).fillX().margin(0f);
                frame.row();
                frame.table(Tex.pane2, blocksSelect -> {
                    blocksSelect.margin(4).marginTop(-1f);
                    blocksSelect.pane(blocks -> blockTable = blocks).height(194f).update(pane -> {
                        if(pane.hasScroll()){
                            Element result = Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true);
                            if(result == null || !result.isDescendantOf(pane)){
                                Core.scene.setScrollFocus(null);
                            }
                        }
                    }).grow().get().setStyle(Styles.smallPane);
                    blocksSelect.row();
                    blocksSelect.table(control.input::buildPlacementUI).name("inputTable").growX();
                }).fillY().bottom().touchable(Touchable.enabled);
                Table categories = new Table();
                categories.bottom();
                categories.add(new Image(Styles.black6){
                    @Override
                    public void draw(){
                        if(height <= Scl.scl(3f)) return;
                        getDrawable().draw(x, y, width, height - Scl.scl(3f));
                    }
                }).colspan(2).growX().growY().padTop(-3f).row();
                categories.defaults().size(50f);

                ButtonGroup<ImageButton> group = new ButtonGroup<>();

                Runnable rebuildCategories = () -> {
                    categories.clearChildren();
                    categoryEmpty = new boolean[ModCategory.size()];
                    //update category empty values
                    for(ModCategory cat : ModCategory.all()){
                        Array<Block> blocks = getByCategory(cat);
                        categoryEmpty[ModCategory.indexOf(cat)] = blocks.isEmpty();
                    }

                    int f = 0;
                    for(ModCategory cat : getCategories()){
                        if(f++ % 2 == 0) categories.row();

                        if(categoryEmpty[ModCategory.indexOf(cat)]){
                            categories.addImage(Styles.black6);
                            continue;
                        }

                        categories.addImageButton(Core.atlas.drawable(cat.icon), Styles.clearToggleTransi, () -> {
                            currentCategory = cat;
                            if(control.input.block != null){
                                control.input.block = getSelectedBlock(currentCategory);
                            }
                            rebuildCategory.run();
                        }).group(group).update(i -> i.setChecked(currentCategory == cat)).name("category-" + cat.name);
                    }
                };
                rebuildCategories.run();

                ScrollPane categoryPane = new ScrollPane(categories);
                categoryPane.setScrollingDisabled(true, false);
                frame.add(categoryPane).height(250f).bottom().touchable(Touchable.enabled);
                rebuildCategory.run();
                frame.update(() -> {
                    if(searchChanged){
                        searchChanged = false;
                        rebuildCategory.run();
                        rebuildCategories.run();
                    }else if(gridUpdate(control.input)) rebuildCategory.run();
                });
            });
        });
    }

    Array<ModCategory> getCategories(){
        returnCatArray.clear();
        returnCatArray.addAll(ModCategory.all());
        returnCatArray.sort((c1, c2) -> Boolean.compare(categoryEmpty[ModCategory.indexOf(c1)], categoryEmpty[ModCategory.indexOf(c2)]));
        return returnCatArray;
    }

    Array<Block> getByCategory(ModCategory cat){
        returnArray.clear();
        String lowerSearch = search.toLowerCase();
        for(Block block : content.blocks()){
            if(((block.category == null && BlockUtils.getByBlock(block) == cat) ^ (block.category != null && ModCategory.get(block.category.name()) == cat)) && block.isVisible() && unlocked(block) && block.localizedName().toLowerCase().contains(lowerSearch)){
                returnArray.add(block);
            }
        }
        returnArray.sort((b1, b2) -> {
            int locked = -Boolean.compare(unlocked(b1), unlocked(b2));
            if(locked != 0) return locked;
            return Boolean.compare(state.rules.bannedBlocks.contains(b1), state.rules.bannedBlocks.contains(b2));
        });
        return returnArray;
    }

    Block getSelectedBlock(ModCategory cat){
        if(selectedBlocks.get(cat) == null){
            selectedBlocks.put(cat, getByCategory(cat).find(this::unlocked));
        }
        return selectedBlocks.get(cat);
    }

    boolean unlocked(Block block){
        return !world.isZone() || data.isUnlocked(block);
    }

    /** Returns the currently displayed block in the top box. */
    Block getSelected(){
        Block toDisplay = null;

        Vector2 v = topTable.stageToLocalCoordinates(Core.input.mouse());

        //setup hovering tile
        if(!Core.scene.hasMouse() && topTable.hit(v.x, v.y, false) == null){
            Tile tile = world.tileWorld(Core.input.mouseWorld().x, Core.input.mouseWorld().y);
            if(tile != null){
                hoverTile = tile.link();
            }else{
                hoverTile = null;
            }
        }else{
            hoverTile = null;
        }

        //block currently selected
        if(control.input.block != null){
            toDisplay = control.input.block;
        }

        //block hovered on in build menu
        if(hovered != null){
            toDisplay = hovered;
        }

        return toDisplay;
    }

    /** Returns the block currently being hovered over in the world. */
    Block tileDisplayBlock(){
        return hoverTile == null ? null : hoverTile.block().synthetic() ? hoverTile.block() : hoverTile.drop() != null ? hoverTile.overlay().itemDrop != null ? hoverTile.overlay() : hoverTile.floor() : null;
    }

    public static void init(){
        try{
            Field f1 = PlacementFragment.class.getDeclaredField("toggler");
            f1.setAccessible(true);
            Group g = new FakeGroup();
            Table tab = new Table();
            g.addChild(tab);
            ((Table)f1.get(ui.hudfrag.blockfrag)).clearChildren();
            f1.set(ui.hudfrag.blockfrag, tab);

            Field f2 = HudFragment.class.getDeclaredField("blockfrag");
            f2.setAccessible(true);
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(f2, f2.getModifiers() & ~Modifier.FINAL);
            f2.set(ui.hudfrag, new ModPlacementFragment());
            ui.hudfrag.blockfrag.build(ui.hudGroup);

            Field f3 = Events.class.getDeclaredField("events");
            f3.setAccessible(true);
            ObjectMap<Object, Array<Cons<?>>> events = (ObjectMap<Object, Array<Cons<?>>>) f3.get(null);
            events.get(WorldLoadEvent.class).remove(events.get(WorldLoadEvent.class).size - 2);
            events.get(UnlockEvent.class).remove(events.get(UnlockEvent.class).size - 2);
            events.get(ResetEvent.class).remove(events.get(ResetEvent.class).size - 2);
        }catch(Exception ex){
            ex.printStackTrace();
            System.exit(0);
        }
    }

    static class FakeGroup extends Group{}
}