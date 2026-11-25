package com.blms.dto;

/** Return type of LeadController.addInteractionNote (D5 Fig 2) = SSD UC07 noteInputForm(). */
public record NoteForm(Long leadId, String companyName) {
}
