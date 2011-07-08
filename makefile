JAVAPATH=
JAVAC=${JAVAPATH}javac
JFLAGS=-target 1.5

DSTRUCTCLASSES=DataStruct/MetaData.class DataStruct/XMLReader.class DataStruct/CMMEParser.class DataStruct/CMMEOldVersionParser.class DataStruct/PieceData.class DataStruct/VariantVersionData.class DataStruct/Voice.class DataStruct/MusicSection.class DataStruct/MusicChantSection.class DataStruct/MusicMensuralSection.class DataStruct/MusicTextSection.class DataStruct/TacetInfo.class DataStruct/EventListData.class DataStruct/VoiceEventListData.class DataStruct/VoiceChantData.class DataStruct/VoiceMensuralData.class DataStruct/Event.class DataStruct/MultiEvent.class DataStruct/ClefEvent.class DataStruct/Pitch.class DataStruct/MensEvent.class DataStruct/RestEvent.class DataStruct/NoteEvent.class DataStruct/DotEvent.class DataStruct/OriginalTextEvent.class DataStruct/CustosEvent.class DataStruct/LineEndEvent.class DataStruct/ProportionEvent.class DataStruct/ColorChangeEvent.class DataStruct/BarlineEvent.class DataStruct/AnnotationTextEvent.class DataStruct/LacunaEvent.class DataStruct/ModernKeySignatureEvent.class DataStruct/VariantMarkerEvent.class DataStruct/Proportion.class DataStruct/Clef.class DataStruct/ClefSet.class DataStruct/Coloration.class DataStruct/MensSignElement.class DataStruct/Mensuration.class DataStruct/ModernAccidental.class DataStruct/ModernKeySignatureElement.class DataStruct/ModernKeySignature.class DataStruct/MIDIReaderWriter.class DataStruct/MusicXMLReader.class DataStruct/Signum.class DataStruct/VariantReading.class DataStruct/EventLocation.class

GFXCLASSES=Gfx/MessageWin.class Gfx/SwingWorker.class Gfx/MusicWin.class Gfx/ViewCanvas.class Gfx/PartsWin.class Gfx/PDFCreator.class Gfx/MusicXMLGenerator.class Gfx/PrintParams.class Gfx/MusicFont.class Gfx/OptionSet.class Gfx/ScoreRenderer.class Gfx/RenderedSectionParams.class Gfx/ScorePageRenderer.class Gfx/RenderedScorePage.class Gfx/RenderedStaffSystem.class Gfx/MeasureList.class Gfx/MeasureInfo.class Gfx/RenderList.class Gfx/RenderedEvent.class Gfx/RenderedClefSet.class Gfx/RenderedSonority.class Gfx/EventImg.class Gfx/EventGlyphImg.class Gfx/EventShapeImg.class Gfx/EventStringImg.class Gfx/RenderParams.class Gfx/PartRenderer.class Gfx/StaffEventData.class Gfx/RenderedEventGroup.class Gfx/GeneralInfoFrame.class Gfx/ScorePagePreviewWin.class Gfx/VariantDisplayFrame.class Gfx/VariantReadingPanel.class Gfx/CriticalNotesWindow.class Gfx/VariantReport.class Gfx/VariantAnalysisList.class Gfx/VariantDisplayOptionsFrame.class Gfx/SelectionPanel.java Gfx/ZoomControl.class Gfx/MIDIPlayer.class

UTILCLASSES=Util/Analyzer.class Util/ConvertCMME.class Util/ProgressInputStream.class Util/ReadCMME.class Util/RecursiveFileList.class

VIEWERCLASSES=Viewer/Main.class

EDITORCLASSES=Editor/Main.class Editor/EditorWin.class Editor/ScoreEditorCanvas.class Editor/ClipboardData.class Editor/GeneralInfoFrame.class Editor/ColorationChooser.class Editor/EditingOptionsFrame.class Editor/MensurationChooser.class Editor/ModernKeySigPanel.class Editor/NoteInfoPanel.class Editor/TextEditorFrame.class Editor/SectionAttribsFrame.class Editor/TextDeleteDialog.class Editor/VariantVersionInfoFrame.class Editor/VariantEditorFrame.class

all: ${DSTRUCTCLASSES} ${GFXCLASSES} ${UTILCLASSES} ${VIEWERCLASSES} ${EDITORCLASSES}


DataStruct/MetaData.class: DataStruct/MetaData.java
	${JAVAC} $(JFLAGS) DataStruct/MetaData.java

DataStruct/XMLReader.class: DataStruct/XMLReader.java
	${JAVAC} $(JFLAGS) DataStruct/XMLReader.java

DataStruct/CMMEParser.class: DataStruct/CMMEParser.java DataStruct/MetaData.class DataStruct/XMLReader.class DataStruct/PieceData.class DataStruct/Event.class DataStruct/MultiEvent.class DataStruct/ClefEvent.class DataStruct/MensEvent.class DataStruct/RestEvent.class DataStruct/NoteEvent.class DataStruct/DotEvent.class DataStruct/ProportionEvent.class DataStruct/ColorChangeEvent.class DataStruct/CustosEvent.class DataStruct/VariantMarkerEvent.class DataStruct/Proportion.class DataStruct/MusicMensuralSection.class DataStruct/VariantReading.class DataStruct/VariantVersionData.class
	${JAVAC} $(JFLAGS) DataStruct/CMMEParser.java

DataStruct/CMMEOldVersionParser.class: DataStruct/CMMEOldVersionParser.java DataStruct/MetaData.class DataStruct/XMLReader.class DataStruct/PieceData.class DataStruct/Event.class DataStruct/MultiEvent.class DataStruct/ClefEvent.class DataStruct/MensEvent.class DataStruct/RestEvent.class DataStruct/NoteEvent.class DataStruct/DotEvent.class DataStruct/ProportionEvent.class DataStruct/ColorChangeEvent.class DataStruct/CustosEvent.class DataStruct/Proportion.class
	${JAVAC} $(JFLAGS) DataStruct/CMMEOldVersionParser.java

DataStruct/MIDIReaderWriter.class: DataStruct/MIDIReaderWriter.java DataStruct/PieceData.class DataStruct/Event.class
	${JAVAC} $(JFLAGS) DataStruct/MIDIReaderWriter.java

DataStruct/MusicXMLReader.class: DataStruct/MusicXMLReader.java DataStruct/PieceData.class DataStruct/XMLReader.class DataStruct/MIDIReaderWriter.class
	${JAVAC} $(JFLAGS) DataStruct/MusicXMLReader.java

DataStruct/PieceData.class: DataStruct/PieceData.java DataStruct/Voice.class DataStruct/MusicSection.class DataStruct/VariantVersionData.class DataStruct/VariantMarkerEvent.class DataStruct/EventLocation.class
	${JAVAC} ${JFLAGS} DataStruct/PieceData.java

DataStruct/VariantVersionData.class: DataStruct/VariantVersionData.java DataStruct/VariantReading.class DataStruct/MusicSection.class
	${JAVAC} ${JFLAGS} DataStruct/VariantVersionData.java

DataStruct/VariantReading.class: DataStruct/VariantReading.java
	${JAVAC} ${JFLAGS} DataStruct/VariantReading.java

DataStruct/Voice.class: DataStruct/Voice.java
	${JAVAC} ${JFLAGS} DataStruct/Voice.java

DataStruct/MusicSection.class: DataStruct/MusicSection.java DataStruct/TacetInfo.class DataStruct/VoiceEventListData.class
	${JAVAC} ${JFLAGS} DataStruct/MusicSection.java

DataStruct/MusicChantSection.class: DataStruct/MusicChantSection.java DataStruct/MusicSection.class
	${JAVAC} ${JFLAGS} DataStruct/MusicChantSection.java

DataStruct/MusicMensuralSection.class: DataStruct/MusicMensuralSection.java DataStruct/MusicSection.class DataStruct/VoiceMensuralData.class
	${JAVAC} ${JFLAGS} DataStruct/MusicMensuralSection.java

DataStruct/MusicTextSection.class: DataStruct/MusicTextSection.java DataStruct/MusicSection.class
	${JAVAC} ${JFLAGS} DataStruct/MusicTextSection.java

DataStruct/TacetInfo.class: DataStruct/TacetInfo.java
	${JAVAC} ${JFLAGS} DataStruct/TacetInfo.java

DataStruct/EventLocation.class: DataStruct/EventLocation.java
	${JAVAC} ${JFLAGS} DataStruct/EventLocation.java

DataStruct/VoiceEventListData.class: DataStruct/VoiceEventListData.java DataStruct/EventListData.class DataStruct/Event.class
	${JAVAC} ${JFLAGS} DataStruct/VoiceEventListData.java

DataStruct/EventListData.class: DataStruct/EventListData.java DataStruct/Event.class
	${JAVAC} ${JFLAGS} DataStruct/EventListData.java

DataStruct/VoiceChantData.class: DataStruct/VoiceChantData.java DataStruct/VoiceEventListData.class
	${JAVAC} ${JFLAGS} DataStruct/VoiceChantData.java

DataStruct/VoiceMensuralData.class: DataStruct/VoiceMensuralData.java DataStruct/VoiceEventListData.class
	${JAVAC} ${JFLAGS} DataStruct/VoiceMensuralData.java

DataStruct/Event.class: DataStruct/Event.java DataStruct/Proportion.class DataStruct/Coloration.class DataStruct/ModernKeySignature.class DataStruct/Signum.class
	${JAVAC} ${JFLAGS} DataStruct/Event.java

DataStruct/MultiEvent.class: DataStruct/MultiEvent.java DataStruct/Event.class
	${JAVAC} ${JFLAGS} DataStruct/MultiEvent.java

DataStruct/Pitch.class: DataStruct/Pitch.java
	${JAVAC} ${JFLAGS} DataStruct/Pitch.java

DataStruct/ClefEvent.class: DataStruct/ClefEvent.java DataStruct/Event.class DataStruct/Clef.class DataStruct/ClefSet.class
	${JAVAC} ${JFLAGS} DataStruct/ClefEvent.java

DataStruct/MensEvent.class: DataStruct/MensEvent.java DataStruct/Event.class DataStruct/MensSignElement.class DataStruct/Mensuration.class
	${JAVAC} ${JFLAGS} DataStruct/MensEvent.java

DataStruct/RestEvent.class: DataStruct/RestEvent.java DataStruct/Event.class DataStruct/NoteEvent.class
	${JAVAC} ${JFLAGS} DataStruct/RestEvent.java

DataStruct/NoteEvent.class: DataStruct/NoteEvent.java DataStruct/Event.class DataStruct/Mensuration.class DataStruct/ModernAccidental.class
	${JAVAC} ${JFLAGS} DataStruct/NoteEvent.java

DataStruct/DotEvent.class: DataStruct/DotEvent.java DataStruct/Event.class
	${JAVAC} ${JFLAGS} DataStruct/DotEvent.java

DataStruct/OriginalTextEvent.class: DataStruct/OriginalTextEvent.java DataStruct/Event.class
	${JAVAC} ${JFLAGS} DataStruct/OriginalTextEvent.java

DataStruct/CustosEvent.class: DataStruct/CustosEvent.java DataStruct/Event.class DataStruct/Pitch.class
	${JAVAC} ${JFLAGS} DataStruct/CustosEvent.java

DataStruct/LineEndEvent.class: DataStruct/LineEndEvent.java DataStruct/Event.class
	${JAVAC} ${JFLAGS} DataStruct/LineEndEvent.java

DataStruct/ProportionEvent.class: DataStruct/ProportionEvent.java DataStruct/Event.class DataStruct/Proportion.class
	${JAVAC} ${JFLAGS} DataStruct/ProportionEvent.java

DataStruct/ColorChangeEvent.class: DataStruct/ColorChangeEvent.java DataStruct/Event.class DataStruct/Coloration.class
	${JAVAC} ${JFLAGS} DataStruct/ColorChangeEvent.java

DataStruct/BarlineEvent.class: DataStruct/BarlineEvent.java DataStruct/Event.class
	${JAVAC} ${JFLAGS} DataStruct/BarlineEvent.java

DataStruct/AnnotationTextEvent.class: DataStruct/AnnotationTextEvent.java DataStruct/Event.class
	${JAVAC} ${JFLAGS} DataStruct/AnnotationTextEvent.java

DataStruct/LacunaEvent.class: DataStruct/LacunaEvent.java DataStruct/Event.class
	${JAVAC} ${JFLAGS} DataStruct/LacunaEvent.java

DataStruct/VariantMarkerEvent.class: DataStruct/VariantMarkerEvent.java DataStruct/Event.class DataStruct/VariantReading.class
	${JAVAC} ${JFLAGS} DataStruct/VariantMarkerEvent.java

DataStruct/ModernKeySignatureEvent.class: DataStruct/ModernKeySignatureEvent.java DataStruct/Event.class DataStruct/ModernKeySignature.class
	${JAVAC} ${JFLAGS} DataStruct/ModernKeySignatureEvent.java

DataStruct/MensSignElement.class: DataStruct/MensSignElement.java
	${JAVAC} ${JFLAGS} DataStruct/MensSignElement.java

DataStruct/Mensuration.class: DataStruct/Mensuration.java
	${JAVAC} ${JFLAGS} DataStruct/Mensuration.java

DataStruct/Proportion.class: DataStruct/Proportion.java
	${JAVAC} ${JFLAGS} DataStruct/Proportion.java

DataStruct/Coloration.class: DataStruct/Coloration.java
	${JAVAC} ${JFLAGS} DataStruct/Coloration.java

DataStruct/Signum.class: DataStruct/Signum.java
	${JAVAC} ${JFLAGS} DataStruct/Signum.java

DataStruct/Clef.class: DataStruct/Clef.java DataStruct/Pitch.class
	${JAVAC} ${JFLAGS} DataStruct/Clef.java

DataStruct/ClefSet.class: DataStruct/ClefSet.java DataStruct/Clef.class
	${JAVAC} ${JFLAGS} DataStruct/ClefSet.java

DataStruct/ModernAccidental.class: DataStruct/ModernAccidental.java
	${JAVAC} ${JFLAGS} DataStruct/ModernAccidental.java

DataStruct/ModernKeySignatureElement.class: DataStruct/ModernKeySignatureElement.java DataStruct/ModernAccidental.class
	${JAVAC} ${JFLAGS} DataStruct/ModernKeySignatureElement.java

DataStruct/ModernKeySignature.class: DataStruct/ModernKeySignature.java DataStruct/ModernKeySignatureElement.class
	${JAVAC} ${JFLAGS} DataStruct/ModernKeySignature.java


Gfx/MessageWin.class: Gfx/MessageWin.java
	${JAVAC} ${JFLAGS} Gfx/MessageWin.java

Gfx/SwingWorker.class: Gfx/SwingWorker.java
	${JAVAC} ${JFLAGS} Gfx/SwingWorker.java

Gfx/SelectionPanel.class: Gfx/SelectionPanel.java DataStruct/VariantVersionData.class
	${JAVAC} ${JFLAGS} Gfx/SelectionPanel.java

Gfx/ZoomControl.class: Gfx/ZoomControl.java
	${JAVAC} ${JFLAGS} Gfx/ZoomControl.java

Gfx/MusicWin.class: Gfx/MusicWin.java Util/ProgressInputStream.class Gfx/MusicFont.class DataStruct/PieceData.class Gfx/OptionSet.class Gfx/ViewCanvas.class Gfx/PartsWin.class Gfx/ZoomControl.class Gfx/MIDIPlayer.class Gfx/VariantAnalysisList.class
	${JAVAC} ${JFLAGS} Gfx/MusicWin.java

Gfx/ViewCanvas.class: Gfx/ViewCanvas.java Gfx/MusicFont.class Gfx/VariantDisplayFrame.class DataStruct/PieceData.class DataStruct/Event.class Gfx/OptionSet.class Gfx/ScoreRenderer.class Gfx/MeasureList.class Gfx/MeasureInfo.class Gfx/RenderList.class Gfx/RenderedSectionParams.class
	${JAVAC} ${JFLAGS} Gfx/ViewCanvas.java

Gfx/PartsWin.class: Gfx/PartsWin.java Gfx/MusicFont.class DataStruct/PieceData.class DataStruct/Event.class Gfx/RenderList.class Gfx/RenderParams.class Gfx/PartRenderer.class Gfx/StaffEventData.class
	${JAVAC} ${JFLAGS} Gfx/PartsWin.java

Gfx/PartRenderer.class: Gfx/PartRenderer.java DataStruct/PieceData.class DataStruct/Event.class Gfx/RenderList.class Gfx/RenderParams.class Gfx/StaffEventData.class Gfx/RenderedEventGroup.class
	${JAVAC} ${JFLAGS} Gfx/PartRenderer.java

Gfx/StaffEventData.class: Gfx/StaffEventData.java DataStruct/Voice.class DataStruct/Event.class Gfx/RenderList.class Gfx/RenderParams.class Gfx/RenderedEventGroup.class
	${JAVAC} ${JFLAGS} Gfx/StaffEventData.java

Gfx/RenderedEventGroup.class: Gfx/RenderedEventGroup.java
	${JAVAC} ${JFLAGS} Gfx/RenderedEventGroup.java

Gfx/GeneralInfoFrame.class: Gfx/GeneralInfoFrame.java Gfx/MusicWin.java DataStruct/PieceData.java
	${JAVAC} ${JFLAGS} Gfx/GeneralInfoFrame.java

Gfx/ScorePagePreviewWin.class: Gfx/ScorePagePreviewWin.java Gfx/ScorePageRenderer.class Gfx/MusicFont.class Gfx/MusicWin.class DataStruct/PieceData.class
	${JAVAC} ${JFLAGS} Gfx/ScorePagePreviewWin.java

Gfx/PDFCreator.class: Gfx/PDFCreator.java Gfx/MusicFont.class Gfx/PrintParams.class Gfx/RenderList.class Gfx/EventImg.class DataStruct/Voice.class DataStruct/PieceData.class
	${JAVAC} ${JFLAGS} Gfx/PDFCreator.java

Gfx/MusicXMLGenerator.class: Gfx/MusicXMLGenerator.java Gfx/ScorePageRenderer.class DataStruct/XMLReader.class DataStruct/MIDIReaderWriter.class
	${JAVAC} $(JFLAGS) Gfx/MusicXMLGenerator.java

Gfx/PrintParams.class: Gfx/PrintParams.java
	${JAVAC} ${JFLAGS} Gfx/PrintParams.java

Gfx/MusicFont.class: Gfx/MusicFont.java DataStruct/Coloration.class DataStruct/NoteEvent.class
	${JAVAC} ${JFLAGS} Gfx/MusicFont.java

Gfx/OptionSet.class: Gfx/OptionSet.java DataStruct/VariantReading.class
	${JAVAC} ${JFLAGS} Gfx/OptionSet.java

Gfx/ScoreRenderer.class: Gfx/ScoreRenderer.java Gfx/MeasureList.class Gfx/MeasureInfo.class Gfx/RenderList.class Gfx/RenderParams.class Gfx/RenderedLigature.class DataStruct/Voice.class DataStruct/Event.class Gfx/RenderedScorePage.class Gfx/RenderedStaffSystem.class Gfx/RenderedSectionParams.class Gfx/RenderedClefSet.class Gfx/RenderedSonority.class
	${JAVAC} ${JFLAGS} Gfx/ScoreRenderer.java

Gfx/RenderedSectionParams.class: Gfx/RenderedSectionParams.java Gfx/RenderedEvent.class Gfx/RenderedClefSet.class
	${JAVAC} ${JFLAGS} Gfx/RenderedSectionParams.java

Gfx/ScorePageRenderer.class: Gfx/ScorePageRenderer.java Gfx/ScoreRenderer.class Gfx/RenderedScorePage.class Gfx/RenderedStaffSystem.class
	${JAVAC} ${JFLAGS} Gfx/ScorePageRenderer.java

Gfx/RenderedScorePage.class: Gfx/RenderedScorePage.java
	${JAVAC} ${JFLAGS} Gfx/RenderedScorePage.java

Gfx/RenderedStaffSystem.class: Gfx/RenderedStaffSystem.java
	${JAVAC} ${JFLAGS} Gfx/RenderedStaffSystem.java

Gfx/MeasureList.class: Gfx/MeasureList.java Gfx/MeasureInfo.class
	${JAVAC} ${JFLAGS} Gfx/MeasureList.java

Gfx/MeasureInfo.class: Gfx/MeasureInfo.java Gfx/RenderedEvent.class Gfx/RenderedClefSet.class
	${JAVAC} ${JFLAGS} Gfx/MeasureInfo.java

Gfx/RenderList.class: Gfx/RenderList.java Gfx/RenderParams.class Gfx/RenderedEvent.class Gfx/OptionSet.class
	${JAVAC} ${JFLAGS} Gfx/RenderList.java

Gfx/RenderedEvent.class: Gfx/RenderedEvent.java Gfx/RenderParams.class Gfx/OptionSet.class DataStruct/Event.class DataStruct/ClefEvent.class DataStruct/MensEvent.class DataStruct/NoteEvent.class DataStruct/RestEvent.class DataStruct/DotEvent.class DataStruct/OriginalTextEvent.class DataStruct/CustosEvent.class DataStruct/LineEndEvent.class Gfx/EventImg.class Gfx/EventGlyphImg.class Gfx/EventShapeImg.class Gfx/EventStringImg.class
	${JAVAC} ${JFLAGS} Gfx/RenderedEvent.java

Gfx/RenderedClefSet.class: Gfx/RenderedClefSet.java Gfx/RenderedEvent.class
	${JAVAC} ${JFLAGS} Gfx/RenderedClefSet.java

Gfx/RenderedSonority.class: Gfx/RenderedSonority.java Gfx/RenderedEvent.class
	${JAVAC} ${JFLAGS} Gfx/RenderedSonority.java

Gfx/RenderParams.class: Gfx/RenderParams.java
	${JAVAC} ${JFLAGS} Gfx/RenderParams.java

Gfx/RenderedLigature.class: Gfx/RenderedLigature.java Gfx/RenderedEventGroup.class DataStruct/NoteEvent.class DataStruct/Pitch.class DataStruct/VoiceEventListData.class
	${JAVAC} ${JFLAGS} Gfx/RenderedLigature.java

Gfx/EventImg.class: Gfx/EventImg.java Gfx/MusicFont.class
	${JAVAC} ${JFLAGS} Gfx/EventImg.java

Gfx/EventGlyphImg.class: Gfx/EventGlyphImg.java Gfx/EventImg.class
	${JAVAC} ${JFLAGS} Gfx/EventGlyphImg.java

Gfx/EventShapeImg.class: Gfx/EventShapeImg.java Gfx/EventImg.class DataStruct/Coloration.class
	${JAVAC} ${JFLAGS} Gfx/EventShapeImg.java

Gfx/EventStringImg.class: Gfx/EventStringImg.java Gfx/EventImg.class DataStruct/Coloration.class
	${JAVAC} ${JFLAGS} Gfx/EventStringImg.java

Gfx/VariantDisplayFrame.class: Gfx/VariantDisplayFrame.java Gfx/VariantReadingPanel.class Gfx/ScoreRenderer.class DataStruct/VariantMarkerEvent.class DataStruct/VoiceEventListData.class
	${JAVAC} ${JFLAGS} Gfx/VariantDisplayFrame.java

Gfx/VariantReadingPanel.class: Gfx/VariantReadingPanel.java Gfx/MusicFont.class Gfx/ScoreRenderer.class DataStruct/VoiceEventListData.class
	${JAVAC} ${JFLAGS} Gfx/VariantReadingPanel.java

Gfx/CriticalNotesWindow.class: Gfx/CriticalNotesWindow.java Gfx/MusicWin.class DataStruct/PieceData.class DataStruct/VariantVersionData.class Gfx/VariantReadingPanel.class Gfx/SelectionPanel.class Gfx/VariantAnalysisList.class
	${JAVAC} ${JFLAGS} Gfx/CriticalNotesWindow.java

Gfx/VariantAnalysisList.class: Gfx/VariantAnalysisList.java Gfx/VariantReport.class DataStruct/PieceData.java
	${JAVAC} ${JFLAGS} Gfx/VariantAnalysisList.java

Gfx/VariantReport.class: Gfx/VariantReport.java DataStruct/PieceData.java
	${JAVAC} ${JFLAGS} Gfx/VariantReport.java

Gfx/VariantDisplayOptionsFrame.class: Gfx/VariantDisplayOptionsFrame.java Gfx/MusicWin.class DataStruct/PieceData.class DataStruct/VariantVersionData.class
	${JAVAC} ${JFLAGS} Gfx/VariantDisplayOptionsFrame.java

Gfx/MIDIPlayer.class: Gfx/MIDIPlayer.java DataStruct/PieceData.class Gfx/ScoreRenderer.class
	${JAVAC} ${JFLAGS} Gfx/MIDIPlayer.java


Util/Analyzer.class: Util/Analyzer.java Gfx/ScoreRenderer.class
	${JAVAC} ${JFLAGS} Util/Analyzer.java

Util/ConvertCMME.class: Util/ConvertCMME.java Util/RecursiveFileList.class DataStruct/XMLReader.class DataStruct/CMMEParser.class DataStruct/CMMEOldVersionParser.class
	${JAVAC} ${JFLAGS} Util/ConvertCMME.java

Util/ProgressInputStream.class: Util/ProgressInputStream.java
	${JAVAC} ${JFLAGS} Util/ProgressInputStream.java

Util/ReadCMME.class: Util/ReadCMME.java DataStruct/XMLReader.class DataStruct/CMMEParser.class
	${JAVAC} ${JFLAGS} Util/ReadCMME.java

Util/RecursiveFileList.class: Util/RecursiveFileList.java
	${JAVAC} ${JFLAGS} Util/RecursiveFileList.java



Viewer/Main.class: Viewer/Main.java DataStruct/MetaData.class Util/ProgressInputStream.class Gfx/MessageWin.class Gfx/SwingWorker.class Gfx/MusicWin.class DataStruct/PieceData.class DataStruct/XMLReader.class DataStruct/CMMEParser.class
	${JAVAC} ${JFLAGS} Viewer/Main.java


Editor/Main.class: Editor/Main.java Editor/EditorWin.class DataStruct/MetaData.class DataStruct/XMLReader.class DataStruct/CMMEParser.class
	${JAVAC} ${JFLAGS} Editor/Main.java

Editor/EditorWin.class: Editor/EditorWin.java Gfx/MusicWin.class Editor/GeneralInfoFrame.class Editor/ColorationChooser.class Editor/EditingOptionsFrame.class Editor/MensurationChooser.class Editor/ModernKeySigPanel.class Editor/NoteInfoPanel.class Editor/TextEditorFrame.class Editor/SectionAttribsFrame.class Editor/VariantVersionInfoFrame.class Gfx/MessageWin.class Viewer/Main.class DataStruct/ModernKeySignatureEvent.class Editor/TextDeleteDialog.class
	${JAVAC} ${JFLAGS} Editor/EditorWin.java

Editor/ScoreEditorCanvas.class: Editor/ScoreEditorCanvas.java Gfx/ViewCanvas.class Editor/EditorWin.class Editor/ClipboardData.class Editor/VariantEditorFrame.class DataStruct/Mensuration.class DataStruct/NoteEvent.class DataStruct/RestEvent.class DataStruct/DotEvent.class DataStruct/LacunaEvent.class DataStruct/ModernKeySignatureEvent.class DataStruct/EventLocation.class
	${JAVAC} ${JFLAGS} Editor/ScoreEditorCanvas.java

Editor/ClipboardData.class: Editor/ClipboardData.java DataStruct/EventListData.java DataStruct/PieceData.java Gfx/ScoreRenderer.java
	${JAVAC} ${JFLAGS} Editor/ClipboardData.java

Editor/GeneralInfoFrame.class: Editor/GeneralInfoFrame.java
	${JAVAC} ${JFLAGS} Editor/GeneralInfoFrame.java

Editor/ColorationChooser.class: Editor/ColorationChooser.java DataStruct/Coloration.class
	${JAVAC} ${JFLAGS} Editor/ColorationChooser.java

Editor/EditingOptionsFrame.class: Editor/EditingOptionsFrame.java
	${JAVAC} ${JFLAGS} Editor/EditingOptionsFrame.java

Editor/MensurationChooser.class: Editor/MensurationChooser.java DataStruct/MensEvent.class
	${JAVAC} ${JFLAGS} Editor/MensurationChooser.java

Editor/ModernKeySigPanel.class: Editor/ModernKeySigPanel.java Gfx/MusicFont.class DataStruct/ModernKeySignature.class
	${JAVAC} ${JFLAGS} Editor/ModernKeySigPanel.java

Editor/NoteInfoPanel.class: Editor/NoteInfoPanel.java Gfx/MusicFont.class Gfx/RenderedEvent.class
	${JAVAC} ${JFLAGS} Editor/NoteInfoPanel.java

Editor/TextDeleteDialog.class: Editor/TextDeleteDialog.java Gfx/SelectionPanel.class
	${JAVAC} ${JFLAGS} Editor/TextDeleteDialog.java

Editor/TextEditorFrame.class: Editor/TextEditorFrame.java
	${JAVAC} ${JFLAGS} Editor/TextEditorFrame.java

Editor/SectionAttribsFrame.class: Editor/SectionAttribsFrame.java DataStruct/MusicSection.java DataStruct/PieceData.java Gfx/SelectionPanel.class
	${JAVAC} ${JFLAGS} Editor/SectionAttribsFrame.java

Editor/VariantVersionInfoFrame.class: Editor/VariantVersionInfoFrame.java DataStruct/PieceData.java
	${JAVAC} ${JFLAGS} Editor/VariantVersionInfoFrame.java

Editor/VariantEditorFrame.class: Editor/VariantEditorFrame.java Gfx/VariantDisplayFrame.class
	${JAVAC} ${JFLAGS} Editor/VariantEditorFrame.java


clean:
	rm ${DSTRUCTCLASSES} ${GFXCLASSES} ${UTILCLASSES} ${VIEWERCLASSES} ${EDITORCLASSES}
