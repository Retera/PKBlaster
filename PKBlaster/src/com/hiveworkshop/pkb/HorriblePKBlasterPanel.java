package com.hiveworkshop.pkb;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;

import com.hiveworkshop.wc3.gui.ExceptionPopup;
import com.hiveworkshop.wc3.gui.util.ColorChooserIcon;
import com.hiveworkshop.wc3.gui.util.ColorChooserIcon.ColorListener;

public class HorriblePKBlasterPanel extends JPanel {
	private static enum OldColor {
		OldRed {
			@Override
			float getValue(final float[] input) {
				return input[ordinal()];
			}
		},
		OldGreen {
			@Override
			float getValue(final float[] input) {
				return input[ordinal()];
			}
		},
		OldBlue {
			@Override
			float getValue(final float[] input) {
				return input[ordinal()];
			}
		},
		OldAlpha {
			@Override
			float getValue(final float[] input) {
				return input[ordinal()];
			}
		},
		Zero {
			@Override
			float getValue(final float[] input) {
				return 0.0f;
			}
		},
		One {
			@Override
			float getValue(final float[] input) {
				return 1.0f;
			}
		};

		abstract float getValue(float[] input);
	};

	private final JFileChooser fileChooser;

	private HorriblePkbParser currentPKB;

	private final DefaultListModel<String> stringListModel;

	private final JList<String> stringsList;

	private final JTextField stringField;

	private final JLabel stringLabel;

	private final DefaultListModel<PKBChunk> nodeListModel;

	private final JList<PKBChunk> nodesList;

	private final JLabel nodeLabel;

	private Color currentColorizeColor = Color.WHITE;

	public HorriblePKBlasterPanel() {
		fileChooser = new JFileChooser(new File("sharedfx"));
		fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Dumb PKB Baked Binary", "pkb"));
		setLayout(new BorderLayout());

		final JTabbedPane tabbedPane = new JTabbedPane();
		add(tabbedPane);

		nodeListModel = new DefaultListModel<>();
		nodesList = new JList<>(nodeListModel);
		final DefaultListCellRenderer indexedCellRenderer = new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
					final boolean isSelected, final boolean cellHasFocus) {
				return super.getListCellRendererComponent(list, "(" + index + ") " + value, index, isSelected,
						cellHasFocus);
			}
		};
		final DefaultListCellRenderer niceNodeCellRenderer = new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
					final boolean isSelected, final boolean cellHasFocus) {
				final String niceName = getNiceName(value);
				final Component listCellRendererComponent = super.getListCellRendererComponent(list,
						"(" + index + ") " + niceName, index, isSelected, cellHasFocus);
				if (niceName.toLowerCase().contains("color") && niceName.contains("LayerCompileCacheField")) {
					setBackground(Color.GREEN);
				}
				if (niceName.contains("NodeSamplerData")) {
					setBackground(Color.PINK);
				}
				return listCellRendererComponent;
			}
		};
		nodesList.setCellRenderer(niceNodeCellRenderer);
		final JPanel editNodePanel = new JPanel();
		nodeLabel = new JLabel("Select a node....");
		editNodePanel.add(nodeLabel);

//		final JPanel unknownChunkPanel = new JPanel();
		final JTable unknownChunkTable = new JTable();
		unknownChunkTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(final JTable table, Object value, final boolean isSelected,
					final boolean hasFocus, final int row, final int column) {
				boolean color = false;
				boolean color2 = false;
				boolean color3 = false;
				if (value instanceof Integer) {
					final Integer x = (Integer) value;
					if ((column == 3) && (((x >= 0) && (x < nodeListModel.size())))) {
						color = true;
						value = value + " (" + getNiceName(nodeListModel.get(x)) + ")";
						if (value.toString().toLowerCase().contains("color")) {
							color3 = true;
						}
					}
					if ((column == 0) && (((x >= 0) && (x < stringListModel.size())))) {
						color2 = true;
						value = value + " (" + stringListModel.get(x) + ")";
					}
				}
				final Component tableCellRendererComponent = super.getTableCellRendererComponent(table, value,
						isSelected, hasFocus, row, column);
				if (color3) {
					setBackground(Color.GREEN);
				} else if (color) {
					setBackground(Color.CYAN);
				} else if (color2) {
					setBackground(Color.YELLOW);
				} else {
					setBackground(null);
				}
				return tableCellRendererComponent;
			}
		});
		final JScrollPane tableScrollPane = new JScrollPane(unknownChunkTable);
		final GroupLayout editNodeLayout = new GroupLayout(editNodePanel);
		editNodePanel.setLayout(editNodeLayout);
		editNodeLayout.setHorizontalGroup(
				editNodeLayout.createParallelGroup().addComponent(nodeLabel).addComponent(tableScrollPane));
		editNodeLayout.setVerticalGroup(
				editNodeLayout.createSequentialGroup().addComponent(nodeLabel).addComponent(tableScrollPane));
		editNodePanel.add(tableScrollPane);

		nodesList.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(final ListSelectionEvent e) {
				final int selectedIndex = nodesList.getSelectedIndex();
				if (selectedIndex == -1) {
					nodeLabel.setText("Select a node....");
				} else {
					final PKBChunk chunk = nodeListModel.getElementAt(selectedIndex);
					nodeLabel.setText("Node " + selectedIndex + ": " + chunk.toString() + " (Length "
							+ chunk.getByteLength() + ")");
					if (chunk instanceof UnknownChunk) {
						unknownChunkTable.setModel(new UnknownChunkTableModel(((UnknownChunk) chunk).getChunkData()));
					}
				}
			}
		});

		final JSplitPane nodesSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(nodesList),
				editNodePanel);
		tabbedPane.addTab("Nodes", nodesSplitPane);

		stringListModel = new DefaultListModel<String>();
		stringsList = new JList<>(stringListModel);
		stringsList.setCellRenderer(indexedCellRenderer);
		final JPanel editStringPanel = new JPanel();
		stringLabel = new JLabel("Select a string....");
		editStringPanel.add(stringLabel);
		stringField = new JTextField(40);
		editStringPanel.add(stringField);
		stringField.setVisible(false);

		stringsList.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(final ListSelectionEvent e) {
				final int selectedIndex = stringsList.getSelectedIndex();
				if (selectedIndex == -1) {
					stringLabel.setText("Select a string....");
					stringField.setVisible(false);
				} else {
					stringField.setVisible(true);
					stringLabel.setText("String " + selectedIndex + ": ");
					stringField.setText(stringListModel.getElementAt(selectedIndex));
				}
			}
		});
		stringField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				final int selectedIndex = stringsList.getSelectedIndex();
				if (selectedIndex != -1) {
					final String newString = stringField.getText();
					stringListModel.setElementAt(newString, selectedIndex);
					currentPKB.setString(selectedIndex, newString);
				}
			}
		});

		final JSplitPane stringsSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(stringsList),
				editStringPanel);
		tabbedPane.addTab("Strings", stringsSplitPane);

		final JTabbedPane automationTabbedPane = new JTabbedPane(JTabbedPane.LEFT);

		{
			final JPanel colorSwap = new JPanel();
			final JLabel introNote1 = new JLabel(
					"This will swap data in the Group 17 or Group 18 of 'CParticleNodeSamplerData_Curve' nodes.");
			colorSwap.add(introNote1);
			final JLabel introNote2 = new JLabel(
					"It might miss data or edit things that aren't colors, but it seems to work pretty well!");
			colorSwap.add(introNote2);
			final JLabel implNote1 = new JLabel(
					"We assume the array of 12 floats for Group 17 is {{RGBA},{RGBA},{RGBA}}");
			colorSwap.add(implNote1);
			implNote1.setFont(implNote1.getFont().deriveFont(8f));
			final JLabel implNote2 = new JLabel(
					"We assume the array of 24 floats for Group 18 is {{RGBA},{RGBA},{RGBA},{RGBA},{RGBA},{RGBA}}");
			implNote2.setFont(implNote2.getFont().deriveFont(8f));
			colorSwap.add(implNote2);

			final JLabel newRedLabel = new JLabel("New Red:");
			final JComboBox<OldColor> newRedSourceBox = new JComboBox<>(OldColor.values());
			newRedSourceBox.setSelectedItem(OldColor.OldRed);
			final Dimension maximumSize = new Dimension(9999, 25);
			newRedSourceBox.setMaximumSize(maximumSize);
			final JLabel newGreenLabel = new JLabel("New Green:");
			final JComboBox<OldColor> newGreenSourceBox = new JComboBox<>(OldColor.values());
			newGreenSourceBox.setSelectedItem(OldColor.OldGreen);
			newGreenSourceBox.setMaximumSize(maximumSize);
			final JLabel newBlueLabel = new JLabel("New Blue:");
			final JComboBox<OldColor> newBlueSourceBox = new JComboBox<>(OldColor.values());
			newBlueSourceBox.setSelectedItem(OldColor.OldBlue);
			newBlueSourceBox.setMaximumSize(maximumSize);
			final JLabel newAlphaLabel = new JLabel("New Alpha:");
			final JComboBox<OldColor> newAlphaSourceBox = new JComboBox<>(OldColor.values());
			newAlphaSourceBox.setSelectedItem(OldColor.OldAlpha);
			newAlphaSourceBox.setMaximumSize(maximumSize);
			final JButton performSwap = new JButton("Perform Swap!");
			performSwap.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					if (currentPKB == null) {
						JOptionPane.showMessageDialog(HorriblePKBlasterPanel.this, "No file loaded!", "Error",
								JOptionPane.ERROR_MESSAGE);
					} else {
						final OldColor newRedIndex = (OldColor) newRedSourceBox.getSelectedItem();
						final OldColor newGreenIndex = (OldColor) newGreenSourceBox.getSelectedItem();
						final OldColor newBlueIndex = (OldColor) newBlueSourceBox.getSelectedItem();
						final OldColor newAlphaIndex = (OldColor) newAlphaSourceBox.getSelectedItem();
						final List<SwappedColor> swappedColors = new ArrayList<>();
						for (final PKBChunk chunk : currentPKB.getChunks()) {
							if (chunk instanceof UnknownChunk) {
								final UnknownChunk c = (UnknownChunk) chunk;
								if (currentPKB.getStrings().get(c.getChunkType())
										.equals("CParticleNodeSamplerData_Curve")) {
									final ByteBuffer data = c.getChunkData();
									data.order(ByteOrder.LITTLE_ENDIAN);
									data.clear();
									final short groupCount = data.getShort();
									System.out.println("Curve with groupCount=" + groupCount);
									for (int groupIndex = 0; groupIndex < groupCount; groupIndex++) {
										final short groupType = data.getShort();
										System.out.println("\tGroup " + groupType);
										switch (groupType) {
										case 0: {
											final int unknown1 = data.getInt();
											final int unknown2 = data.getInt();
											final int nameStringIndex = data.getInt();
											System.out.println("\t\tUnknown1: " + unknown1);
											System.out.println("\t\tUnknown2: " + unknown2);
											System.out.println(
													"\t\tName: " + currentPKB.getStrings().get(nameStringIndex));
											break;
										}
										case 7: {
											final int unknown = data.getInt();
											final int propertyIndex = data.getInt();
											System.out.println("\t\tUnknown: " + unknown);
											System.out.println("\t\tPropertyIndex: " + propertyIndex);
											break;
										}
										case 9: {
											final int unknown = data.getInt();
											System.out.println("\t\tUnknown: " + unknown);
											break;
										}
										case 16: {
											final int numberOfFloats = data.getInt();
											System.out.println("\t\tnumberOfFloats: " + numberOfFloats);
											final float[] floats = new float[numberOfFloats];
											for (int i = 0; i < numberOfFloats; i++) {
												floats[i] = data.getFloat();
											}
											System.out.println("\t\t: " + Arrays.toString(floats));
											break;
										}
										case 17: {
											final int numberOfFloats = data.getInt();
											System.out.println("\t\tnumberOfFloats: " + numberOfFloats);
											if (numberOfFloats == 12) {
												final int floatStartPos = data.position();
												for (int i = 0; i < 3; i++) {
													final float oldRed = data.getFloat(floatStartPos + (i * 16) + 0);
													final float oldGreen = data.getFloat(floatStartPos + (i * 16) + 4);
													final float oldBlue = data.getFloat(floatStartPos + (i * 16) + 8);
													final float oldAlpha = data.getFloat(floatStartPos + (i * 16) + 12);
													final float[] oldColors = { oldRed, oldGreen, oldBlue, oldAlpha };
													final float newRed = newRedIndex.getValue(oldColors);
													data.putFloat(floatStartPos + (i * 16), newRed);
													final float newGreen = newGreenIndex.getValue(oldColors);
													data.putFloat(floatStartPos + (i * 16) + 4, newGreen);
													final float newBlue = newBlueIndex.getValue(oldColors);
													data.putFloat(floatStartPos + (i * 16) + 8, newBlue);
													final float newAlpha = newAlphaIndex.getValue(oldColors);
													data.putFloat(floatStartPos + (i * 16) + 12, newAlpha);
													swappedColors.add(new SwappedColor(
															createColor(oldRed, oldGreen, oldBlue, oldAlpha),
															createColor(newRed, newGreen, newBlue, newAlpha)));

												}
												data.position(floatStartPos + (3 * 16));
											} else {
												final float[] floats = new float[numberOfFloats];
												for (int i = 0; i < numberOfFloats; i++) {
													floats[i] = data.getFloat();
												}
												System.out.println("\t\t: " + Arrays.toString(floats));
											}
											break;
										}
										case 18: {
											final int numberOfFloats = data.getInt();
											System.out.println("\t\tnumberOfFloats: " + numberOfFloats);
											if (numberOfFloats == 24) {
												final int floatStartPos = data.position();
												for (int i = 0; i < 6; i++) {
													final float oldRed = data.getFloat(floatStartPos + (i * 16) + 0);
													final float oldGreen = data.getFloat(floatStartPos + (i * 16) + 4);
													final float oldBlue = data.getFloat(floatStartPos + (i * 16) + 8);
													final float oldAlpha = data.getFloat(floatStartPos + (i * 16) + 12);
													final float[] oldColors = { oldRed, oldGreen, oldBlue, oldAlpha };
													final float newRed = newRedIndex.getValue(oldColors);
													data.putFloat(floatStartPos + (i * 16), newRed);
													final float newGreen = newGreenIndex.getValue(oldColors);
													data.putFloat(floatStartPos + (i * 16) + 4, newGreen);
													final float newBlue = newBlueIndex.getValue(oldColors);
													data.putFloat(floatStartPos + (i * 16) + 8, newBlue);
													final float newAlpha = newAlphaIndex.getValue(oldColors);
													data.putFloat(floatStartPos + (i * 16) + 12, newAlpha);
													swappedColors.add(new SwappedColor(
															createColor(oldRed, oldGreen, oldBlue, oldAlpha),
															createColor(newRed, newGreen, newBlue, newAlpha)));
												}
												data.position(floatStartPos + (24 * 4));
											} else {
												final float[] floats = new float[numberOfFloats];
												for (int i = 0; i < numberOfFloats; i++) {
													floats[i] = data.getFloat();
												}
												System.out.println("\t\t: " + Arrays.toString(floats));
											}
											break;
										}
										default:
											throw new IllegalStateException("Unknown group type: " + groupType);
										}
									}
									data.clear();
								}
							}
						}
						final ColorSwapPreviewPanel colorSwapPreviewPanel = new ColorSwapPreviewPanel(swappedColors);
						final JScrollPane preview = new JScrollPane(colorSwapPreviewPanel);
						preview.setPreferredSize(new Dimension(800, 600));
						JOptionPane.showMessageDialog(HorriblePKBlasterPanel.this, preview);
					}
				}
			});

			final GroupLayout colorSwapLayout = new GroupLayout(colorSwap);
			colorSwapLayout.setHorizontalGroup(colorSwapLayout.createParallelGroup().addComponent(introNote1)
					.addComponent(introNote2).addComponent(implNote1).addComponent(implNote2)
					.addGroup(colorSwapLayout.createSequentialGroup().addComponent(newRedLabel)
							.addComponent(newRedSourceBox))
					.addGroup(colorSwapLayout.createSequentialGroup().addComponent(newGreenLabel)
							.addComponent(newGreenSourceBox))
					.addGroup(colorSwapLayout.createSequentialGroup().addComponent(newBlueLabel)
							.addComponent(newBlueSourceBox))
					.addGroup(colorSwapLayout.createSequentialGroup().addComponent(newAlphaLabel)
							.addComponent(newAlphaSourceBox))
					.addComponent(performSwap));
			colorSwapLayout.setVerticalGroup(colorSwapLayout.createSequentialGroup().addComponent(introNote1)
					.addComponent(introNote2).addComponent(implNote1).addComponent(implNote2)
					.addGroup(colorSwapLayout.createParallelGroup().addComponent(newRedLabel)
							.addComponent(newRedSourceBox))
					.addGroup(colorSwapLayout.createParallelGroup().addComponent(newGreenLabel)
							.addComponent(newGreenSourceBox))
					.addGroup(colorSwapLayout.createParallelGroup().addComponent(newBlueLabel)
							.addComponent(newBlueSourceBox))
					.addGroup(colorSwapLayout.createParallelGroup().addComponent(newAlphaLabel)
							.addComponent(newAlphaSourceBox))
					.addComponent(performSwap));
			colorSwap.setLayout(colorSwapLayout);

			automationTabbedPane.addTab("Sampler Data Color Swap", colorSwap);
		}

		{
			final JPanel colorize = new JPanel();
			final JLabel introNote1 = new JLabel(
					"This will overwrite data in the Group 17 or Group 18 of 'CParticleNodeSamplerData_Curve' nodes.");
			colorize.add(introNote1);
			final JLabel introNote2 = new JLabel(
					"It might miss data or edit things that aren't colors, but it seems to work pretty well!");
			colorize.add(introNote2);
			final JLabel implNote1 = new JLabel(
					"We assume the array of 12 floats for Group 17 is {{RGBA},{RGBA},{RGBA}}");
			colorize.add(implNote1);
			implNote1.setFont(implNote1.getFont().deriveFont(8f));
			final JLabel implNote2 = new JLabel(
					"We assume the array of 24 floats for Group 18 is {{RGBA},{RGBA},{RGBA},{RGBA},{RGBA},{RGBA}}");
			implNote2.setFont(implNote2.getFont().deriveFont(8f));
			colorize.add(implNote2);

			final ColorChooserIcon colorChooserIcon = new ColorChooserIcon(currentColorizeColor, new ColorListener() {
				@Override
				public void colorChanged(final Color color) {
					currentColorizeColor = color;
				}
			});
			final JLabel newRedLabel = new JLabel("New Color:");
			final JButton performSwap = new JButton("Perform Colorize!");
			performSwap.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					if (currentPKB == null) {
						JOptionPane.showMessageDialog(HorriblePKBlasterPanel.this, "No file loaded!", "Error",
								JOptionPane.ERROR_MESSAGE);
					} else {
						final List<SwappedColor> swappedColors = new ArrayList<>();
						for (final PKBChunk chunk : currentPKB.getChunks()) {
							if (chunk instanceof UnknownChunk) {
								final UnknownChunk c = (UnknownChunk) chunk;
								if (currentPKB.getStrings().get(c.getChunkType())
										.equals("CParticleNodeSamplerData_Curve")) {
									final ByteBuffer data = c.getChunkData();
									data.order(ByteOrder.LITTLE_ENDIAN);
									data.clear();
									final short groupCount = data.getShort();
									System.out.println("Curve with groupCount=" + groupCount);
									for (int groupIndex = 0; groupIndex < groupCount; groupIndex++) {
										final short groupType = data.getShort();
										System.out.println("\tGroup " + groupType);
										switch (groupType) {
										case 0: {
											final int unknown1 = data.getInt();
											final int unknown2 = data.getInt();
											final int nameStringIndex = data.getInt();
											System.out.println("\t\tUnknown1: " + unknown1);
											System.out.println("\t\tUnknown2: " + unknown2);
											System.out.println(
													"\t\tName: " + currentPKB.getStrings().get(nameStringIndex));
											break;
										}
										case 7: {
											final int unknown = data.getInt();
											final int propertyIndex = data.getInt();
											System.out.println("\t\tUnknown: " + unknown);
											System.out.println("\t\tPropertyIndex: " + propertyIndex);
											break;
										}
										case 9: {
											final int unknown = data.getInt();
											System.out.println("\t\tUnknown: " + unknown);
											break;
										}
										case 16: {
											final int numberOfFloats = data.getInt();
											System.out.println("\t\tnumberOfFloats: " + numberOfFloats);
											final float[] floats = new float[numberOfFloats];
											for (int i = 0; i < numberOfFloats; i++) {
												floats[i] = data.getFloat();
											}
											System.out.println("\t\t: " + Arrays.toString(floats));
											break;
										}
										case 17: {
											final int numberOfFloats = data.getInt();
											System.out.println("\t\tnumberOfFloats: " + numberOfFloats);
											if (numberOfFloats == 12) {
												final int floatStartPos = data.position();
												for (int i = 0; i < 3; i++) {
													final float oldRed = data.getFloat(floatStartPos + (i * 16) + 0);
													final float oldGreen = data.getFloat(floatStartPos + (i * 16) + 4);
													final float oldBlue = data.getFloat(floatStartPos + (i * 16) + 8);
													final float avgColor = (oldRed + oldGreen + oldBlue) / 3;
													final float newFactor = Math.signum(avgColor)
															* Math.max(Math.max(Math.abs(oldRed), Math.abs(oldGreen)),
																	Math.abs(oldBlue));

													final float oldAlpha = data.getFloat(floatStartPos + (i * 16) + 12);
													final float newRed = (newFactor * currentColorizeColor.getRed())
															/ 255f;
													data.putFloat(floatStartPos + (i * 16), newRed);
													final float newGreen = (newFactor * currentColorizeColor.getGreen())
															/ 255f;
													data.putFloat(floatStartPos + (i * 16) + 4, newGreen);
													final float newBlue = (newFactor * currentColorizeColor.getBlue())
															/ 255f;
													data.putFloat(floatStartPos + (i * 16) + 8, newBlue);
													swappedColors.add(new SwappedColor(
															createColor(oldRed, oldGreen, oldBlue, oldAlpha),
															createColor(newRed, newGreen, newBlue, oldAlpha)));

												}
												data.position(floatStartPos + (3 * 16));
											} else {
												final float[] floats = new float[numberOfFloats];
												for (int i = 0; i < numberOfFloats; i++) {
													floats[i] = data.getFloat();
												}
												System.out.println("\t\t: " + Arrays.toString(floats));
											}
											break;
										}
										case 18: {
											final int numberOfFloats = data.getInt();
											System.out.println("\t\tnumberOfFloats: " + numberOfFloats);
											if (numberOfFloats == 24) {
												final int floatStartPos = data.position();
												for (int i = 0; i < 6; i++) {
													final float oldRed = data.getFloat(floatStartPos + (i * 16) + 0);
													final float oldGreen = data.getFloat(floatStartPos + (i * 16) + 4);
													final float oldBlue = data.getFloat(floatStartPos + (i * 16) + 8);
													final float avgColor = (oldRed + oldGreen + oldBlue) / 3;
													final float newFactor = Math.signum(avgColor)
															* Math.max(Math.max(Math.abs(oldRed), Math.abs(oldGreen)),
																	Math.abs(oldBlue));

													final float oldAlpha = data.getFloat(floatStartPos + (i * 16) + 12);
													final float newRed = (newFactor * currentColorizeColor.getRed())
															/ 255f;
													data.putFloat(floatStartPos + (i * 16), newRed);
													final float newGreen = (newFactor * currentColorizeColor.getGreen())
															/ 255f;
													data.putFloat(floatStartPos + (i * 16) + 4, newGreen);
													final float newBlue = (newFactor * currentColorizeColor.getBlue())
															/ 255f;
													data.putFloat(floatStartPos + (i * 16) + 8, newBlue);
													swappedColors.add(new SwappedColor(
															createColor(oldRed, oldGreen, oldBlue, oldAlpha),
															createColor(newRed, newGreen, newBlue, oldAlpha)));
												}
												data.position(floatStartPos + (24 * 4));
											} else {
												final float[] floats = new float[numberOfFloats];
												for (int i = 0; i < numberOfFloats; i++) {
													floats[i] = data.getFloat();
												}
												System.out.println("\t\t: " + Arrays.toString(floats));
											}
											break;
										}
										default:
											throw new IllegalStateException("Unknown group type: " + groupType);
										}
									}
									data.clear();
								}
							}
						}
						final ColorSwapPreviewPanel colorSwapPreviewPanel = new ColorSwapPreviewPanel(swappedColors);
						final JScrollPane preview = new JScrollPane(colorSwapPreviewPanel);
						preview.setPreferredSize(new Dimension(800, 600));
						JOptionPane.showMessageDialog(HorriblePKBlasterPanel.this, preview);
					}
				}
			});

			final GroupLayout colorSwapLayout = new GroupLayout(colorize);
			colorSwapLayout.setHorizontalGroup(colorSwapLayout.createParallelGroup().addComponent(introNote1)
					.addComponent(introNote2).addComponent(implNote1).addComponent(implNote2).addGroup(colorSwapLayout
							.createSequentialGroup().addComponent(newRedLabel).addComponent(colorChooserIcon))
					.addComponent(performSwap));
			colorSwapLayout.setVerticalGroup(colorSwapLayout.createSequentialGroup().addComponent(introNote1)
					.addComponent(introNote2).addComponent(implNote1).addComponent(implNote2).addGroup(colorSwapLayout
							.createParallelGroup().addComponent(newRedLabel).addComponent(colorChooserIcon))
					.addComponent(performSwap));
			colorize.setLayout(colorSwapLayout);

			automationTabbedPane.addTab("Sampler Data Colorize", colorize);
		}

		tabbedPane.addTab("Automation", automationTabbedPane);

	}

	private String getNiceName(final Object value) {
		final PKBChunk value2 = (PKBChunk) value;
		String suffix = "";
		if ((value2.getByteLength() >= 8) && (value2 instanceof UnknownChunk)) {
			final ByteBuffer chunkData = ((UnknownChunk) value2).getChunkData();
			chunkData.order(ByteOrder.LITTLE_ENDIAN);
			final int chunkDataValue = chunkData.getInt(4);
			if ((chunkDataValue >= 0) && (chunkDataValue < stringListModel.size())) {
				suffix = " \"" + stringListModel.get(chunkDataValue) + "\"";
			}
		}
		final String niceName = stringListModel.get(value2.getChunkType()) + suffix;
		return niceName;
	}

	protected void populateUI() {
		stringListModel.clear();
		for (final String s : currentPKB.getStrings()) {
			stringListModel.addElement(s);
		}
		nodeListModel.clear();
		for (final PKBChunk chunk : currentPKB.getChunks()) {
			nodeListModel.addElement(chunk);
		}
	}

	public JMenuBar createJMenuBar() {
		final JMenuBar menuBar = new JMenuBar();

		final JMenu fileMenu = new JMenu("File");
		menuBar.add(fileMenu);

		final JMenuItem openItem = new JMenuItem("Open");
		openItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				if (fileChooser.showOpenDialog(HorriblePKBlasterPanel.this) == JFileChooser.APPROVE_OPTION) {
					final File selectedFile = fileChooser.getSelectedFile();
					if (selectedFile != null) {

						final ByteBuffer stupidBuffer2 = ByteBuffer.allocate((int) selectedFile.length());
						try (FileChannel channel = FileChannel.open(selectedFile.toPath(), StandardOpenOption.READ)) {
							channel.read(stupidBuffer2);
							stupidBuffer2.clear();
							currentPKB = new HorriblePkbParser(stupidBuffer2);
							populateUI();
						} catch (final IOException e1) {
							e1.printStackTrace();
							ExceptionPopup.display(e1);
						}
					}
				}
			}
		});
		fileMenu.add(openItem);

		final JMenuItem saveItem = new JMenuItem("Save");
		saveItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				if (fileChooser.showSaveDialog(HorriblePKBlasterPanel.this) == JFileChooser.APPROVE_OPTION) {
					final File selectedFile = fileChooser.getSelectedFile();
					if (selectedFile != null) {

						final ByteBuffer stupidBuffer2 = currentPKB.toBuffer();
						try (FileChannel channel = FileChannel.open(selectedFile.toPath(),
								StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
							stupidBuffer2.clear();
							channel.write(stupidBuffer2);
						} catch (final IOException e1) {
							e1.printStackTrace();
							ExceptionPopup.display(e1);
						}
					}
				}
			}
		});
		fileMenu.add(saveItem);

		return menuBar;
	}

	private Color createColor(final float oldRed, final float oldGreen, final float oldBlue, final float oldAlpha) {
		return new Color(Math.max(0f, Math.min(1.0f, oldRed)), Math.max(0f, Math.min(1.0f, oldGreen)),
				Math.max(0f, Math.min(1.0f, oldBlue)), Math.max(0f, Math.min(1.0f, oldAlpha)));
	}

}
