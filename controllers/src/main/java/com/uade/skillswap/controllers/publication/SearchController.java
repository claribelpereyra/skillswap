package com.uade.skillswap.controllers.publication;

import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.dto.publication.PublicationSummary;
import com.uade.skillswap.controllers.ControllerResult;

public interface SearchController {

    ControllerResult<Page<PublicationSummary>> search(SearchForm form);
}
