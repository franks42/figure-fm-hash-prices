# iTerm2 + Amp Usage Guide

## Problem Summary
- Content scrolls off screen and can't be retrieved
- Mixed session content from previous conversations
- Difficulty navigating long AI assistant outputs

## iTerm2 Scrollback Configuration

### Essential Settings
1. **Open iTerm2 Preferences** (`Cmd + ,`)
2. **Go to Profiles → Terminal**
3. **Scrollback Buffer Settings**:
   - ✅ **Check "Unlimited scrollback"** (should already be enabled)
   - ✅ **"Scrollback with status bar": ON**
   - ✅ **"Save lines to scrollback when an app status bar is present": ON**

### Navigation Shortcuts
- **`Cmd + ↑`** - Scroll to top of buffer
- **`Cmd + Shift + ↑`** - Jump to very top instantly  
- **`Cmd + ↓`** - Scroll to bottom
- **`Fn + ↑/↓`** - Page up/down through history
- **`Cmd + F`** - Search within scrollback buffer
- **`Cmd + Shift + ←/→`** - Jump between command blocks

### Session Management
- **`Cmd + K`** - Clear scrollback buffer (fresh start)
- **`Cmd + R`** - Clear current screen only (keeps scrollback)
- **`Cmd + Shift + M`** - Set mark at current position
- **`Cmd + Shift + J`** - Jump to previous mark

## Amp-Specific Optimizations

### Profile Settings for AI Sessions
**Keys Settings** (`Preferences → Profiles → Keys`):
```
✅ Option as Meta key: ON (better text navigation)
✅ Mouse reporting: OFF (better copy/paste)
✅ Applications may access clipboard: ON
```

**Advanced Settings** (`Preferences → Advanced → Terminal`):
```
✅ Scroll wheel sends arrow keys when in alternate screen mode: NO
✅ Mouse wheel scrolls when mouse is over scrollbar: YES
✅ Minimum contrast: 0.3 (better text readability)
```

### Window Settings
**Window Settings** (`Preferences → Profiles → Window`):
```
✅ Transparency: 0% (better readability)
✅ Blur: OFF (performance)
✅ Use transparency: OFF
```

## Workflow Tips for Long AI Sessions

### Before Starting Major Tasks
1. **Clear buffer**: `Cmd + K` 
2. **Set initial mark**: `Cmd + Shift + M`
3. **Note session purpose** in terminal title

### During Long Conversations
1. **Search for specific content**: `Cmd + F` + search term
2. **Mark important responses**: `Cmd + Shift + M`
3. **Copy important code/commands immediately**

### Content Management
1. **Export important sessions**: `File → Print → Save as PDF`
2. **Copy long outputs to external files**:
   ```bash
   pbpaste > important_output.txt
   ```
3. **Use external editor for planning**:
   ```bash
   code notes.md  # VS Code
   vim notes.md   # Vim
   ```

## Alternative Solutions

### Terminal Multiplexer (Advanced)
```bash
# Install tmux for better session management
brew install tmux

# Start dedicated Amp session
tmux new-session -s amp-session

# Detach/reattach sessions
# Ctrl+B, D (detach)
tmux attach -t amp-session
```

### Dedicated Amp Terminal Profile
1. **Create new profile**: `Preferences → Profiles → +`
2. **Name**: "Amp AI Assistant" 
3. **Settings**:
   - Unlimited scrollback: ON
   - Larger font (14pt+)
   - High contrast colors
   - Different background color
   - Wider window (120+ columns)

### External Tools Integration
```bash
# Save command outputs directly to files
amp-output | tee important_session.txt

# Use less for long outputs  
long-command | less

# Search through saved sessions
grep "specific term" *.txt
```

## Troubleshooting Common Issues

### "Can't scroll back to see earlier content"
- **Solution**: Content may have exceeded buffer limits despite "unlimited" setting
- **Fix**: Increase system memory allocation to iTerm2
- **Workaround**: Save important outputs immediately

### "Seeing content from previous sessions"  
- **Solution**: `Cmd + K` to clear buffer between sessions
- **Prevention**: Create separate terminal tabs for different tasks

### "Copy/paste not working properly"
- **Solution**: Disable mouse reporting in Keys settings
- **Alternative**: Use `Cmd + C` instead of mouse selection

### "Interface feels sluggish with long outputs"
- **Solution**: Reduce visual effects (transparency, blur)
- **Alternative**: Use simpler color scheme
- **Advanced**: Increase iTerm2 memory allocation

## Best Practices for Amp Usage

1. **Start each major task with clean buffer** (`Cmd + K`)
2. **Use search instead of scrolling** for finding content (`Cmd + F`)
3. **Mark important points** during conversation (`Cmd + Shift + M`) 
4. **Save critical outputs immediately** (copy to files)
5. **Use dedicated terminal profile** for AI sessions
6. **Export session transcripts** for later reference
7. **Keep external notes** for tracking todos and progress

## Quick Reference Card

| Action | Shortcut |
|--------|----------|
| Clear scrollback | `Cmd + K` |
| Search buffer | `Cmd + F` |
| Jump to top | `Cmd + Shift + ↑` |
| Jump to bottom | `Cmd + Shift + ↓` |
| Set mark | `Cmd + Shift + M` |
| Previous mark | `Cmd + Shift + J` |
| Page up | `Fn + ↑` |
| Page down | `Fn + ↓` |
| Copy selection | `Cmd + C` |
| Preferences | `Cmd + ,` |

## Memory & Performance

### If iTerm2 Uses Too Much Memory
1. **Reduce scrollback lines** to 50,000-100,000 instead of unlimited
2. **Clear buffer regularly** during long sessions
3. **Close unused tabs/windows**  
4. **Restart iTerm2** periodically for memory cleanup

### For Better Performance
1. **Turn off visual effects** (transparency, blur)
2. **Use bitmap fonts** instead of vector fonts
3. **Reduce color depth** if using custom themes
4. **Disable unnecessary features** in Advanced settings

---

*This guide is specifically for using Sourcegraph Amp AI assistant effectively with iTerm2 on macOS.*
